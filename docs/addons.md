# Writing addons for Care For Them

Most content can be added with a [datapack](datapacks/index.md): new social classes, needs, jobs and
structures, using the types CFT provides. An addon mod goes one step further and adds new
*types* in Java (a new kind of need, job or structure), which datapacks then configure like the
built-in ones. It can also make Xoonglins do new things, through the same extension points CFT
uses for its own content, without changing CFT.

An addon is a regular NeoForge mod that depends on CFT (mod id `cft`): declare the dependency in
your `neoforge.mods.toml` and add the CFT jar to your build's dependencies.

## How it fits together

- **Data**: social classes, needs, jobs and structure types are datapack registries. Their files
  go in `data/<namespace>/cft/socialclass/`, `.../cft/need/`, `.../cft/job/` and
  `.../cft/structure/`, and each file's id comes from its name, as with any datapack registry:
  `data/myaddon/cft/need/campfire_need.json` is `myaddon:campfire_need`. A social class lists the
  needs and jobs of its Xoonglins by id.
- **Types**: the `"type"` field of each need, job or structure file picks the codec that reads it,
  from one of three type registries: `cft:need_serializer`, `cft:job_serializer` and
  `cft:structure_serializer`. Adding a type means registering a codec there.

## Registering types

Register your codecs with a `DeferredRegister` on the matching registry key, from
`CftDatapackRegistryEvents`, and register it on your mod's event bus:

```java
public static final DeferredRegister<Codec<? extends Need>> NEED_TYPES =
        DeferredRegister.create(CftDatapackRegistryEvents.NEED_CODEC_KEY, MyAddon.MOD_ID);
public static final DeferredHolder<Codec<? extends Need>, Codec<WarmthNeed>> WARMTH_NEED =
        NEED_TYPES.register("warmth", () -> WarmthNeed.CODEC);

// In your mod's constructor
NEED_TYPES.register(modEventBus);
```

Datapack files then use `"type": "myaddon:warmth"`. Jobs use `JOB_CODEC_KEY` and structure types
`STRUCTURE_TYPE_CODEC_KEY` the same way.

The codecs must encode as well as decode: CFT saves each Xoonglin's needs through them, and the
registries are synced to clients with them.

## Adding a need type

A need type is two classes:

- **`Need`**: the configuration, read from JSON. Its constructor takes the fields every need has
  (`damage`, `damage_threshold`, `provided_happiness`, `satisfaction_threshold`,
  `frequency`, `hidden`, `bonus` and `icon`), so declare those in your codec along with your own
  fields. Implement:
    - `needType()`: your registered codec.
    - `createSatisfier(satisfaction, isSatisfied)`: a new satisfier for a Xoonglin.
    - `getDefaultIcons()`: item ids shown as the need's icon when the JSON sets no `icon`.
    - `getTypeName()`: the type's name, shown in the need's tooltip.
- **`NeedSatisfier`**: the state of the need in one Xoonglin. CFT checks each need once per second
  on the server. Below the need's `satisfaction_threshold`, it calls `satisfy(mob)`; otherwise it
  calls `unsatisfy(frequency, mob)`, which wears satisfaction off (and hurts the Xoonglin once it
  drops below `damage_threshold`, if the need has `damage`). Your satisfier implements:
    - `satisfy(mob)`: if the need can be met right now, meet it (e.g. consume an item) and return
    `super.satisfy(mob)`, which refills satisfaction and adds happiness. Otherwise, by convention,
    call `unsatisfy(getNeed().getFrequency(), mob)`, `mob.decreaseHappiness(getNeed())` and
    `addMemoriesForSatisfaction(mob)`, and return `false`.
    - `addMemoriesForSatisfaction(mob)`: set the memories that send the Xoonglin to meet the need.

  Use `getNeed()` to reach your need's configuration from the satisfier, and `getNeedId()` for its
  id.

Needs that are met instantly (e.g. by being near something) only need those two classes. When the
Xoonglin has to go somewhere, the need also needs a memory and a behavior, and it chooses when that
happens:

- **Right away, even at work**: start an [errand](#errands). Its job is paused and it goes to
  `INVESTIGATE`, where your behavior runs. Fetching supplies works like this.
- **In its free time**: add your behavior to `IDLE`, and don't start an errand. Visiting a tavern
  works like this.

### Example: a warmth need

A Xoonglin wants to warm up by a campfire now and then. It's satisfied while it stands next to a
lit campfire; otherwise it leaves whatever it's doing to walk to the nearest one.

```java
public class WarmthNeed extends Need {
    public static final Codec<WarmthNeed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("damage").forGetter(Need::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(Need::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(Need::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(Need::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(Need::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Need::isHidden),
            Codec.BOOL.optionalFieldOf("bonus", false).forGetter(Need::isBonus),
            Codec.INT.optionalFieldOf("search_radius", 32).forGetter(WarmthNeed::getSearchRadius),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(Need::getIcon)
    ).apply(instance, WarmthNeed::new));

    private final int searchRadius;

    public WarmthNeed(double damage, double damageThreshold, double providedHappiness,
                      double satisfactionThreshold, double frequency, boolean hidden, boolean bonus,
                      int searchRadius, Optional<ResourceLocation> icon) {
        super(damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden, bonus, icon);
        this.searchRadius = searchRadius;
    }

    public int getSearchRadius() { return searchRadius; }

    @Override public Codec<? extends Need> needType() { return MyAddonRegistry.WARMTH_NEED.get(); }
    @Override public NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new WarmthNeedSatisfier(satisfaction, isSatisfied, this);
    }
    @Override public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("campfire"));
    }
    @Override public String getTypeName() {
        return Component.translatable("gui.myaddon.need_type.warmth").getString();
    }
}
```

```java
public class WarmthNeedSatisfier extends NeedSatisfier<WarmthNeed> {
    public static final ResourceLocation WARM_UP = ResourceLocation.fromNamespaceAndPath("myaddon", "warm_up");

    public WarmthNeedSatisfier(double satisfaction, boolean isSatisfied, WarmthNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (mob.level().isClientSide) return isSatisfied();
        if (CampfireUtil.isNextToLitCampfire(mob)) {
            return super.satisfy(mob);
        }
        unsatisfy(getNeed().getFrequency(), mob);
        mob.decreaseHappiness(getNeed());
        addMemoriesForSatisfaction(mob);
        return false;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        CampfireUtil.findLitCampfire(mob, getNeed().getSearchRadius()).ifPresent(pos -> {
            mob.getBrain().setMemory(MyAddonMemories.CAMPFIRE.get(), pos);
            ErrandUtils.start(mob, WARM_UP);
        });
    }
}
```

The behavior walks the Xoonglin to the campfire, and finishes the errand however it ends, so its
job resumes:

```java
public class WarmUpBehavior extends Behavior<XoonglinEntity> {
    public WarmUpBehavior() {
        // Walking behaviors need a real maximum duration: vanilla's default is 3 seconds
        super(Map.of(MyAddonMemories.CAMPFIRE.get(), MemoryStatus.VALUE_PRESENT), 1200);
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity mob, long gameTime) {
        BlockPos campfire = mob.getBrain().getMemory(MyAddonMemories.CAMPFIRE.get()).orElseThrow();
        mob.getNavigation().moveTo(campfire.getX() + 0.5, campfire.getY(), campfire.getZ() + 0.5, 1.0);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity mob, long gameTime) {
        return !CampfireUtil.isNextToLitCampfire(mob) && !mob.getNavigation().isDone();
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity mob, long gameTime) {
        mob.getBrain().eraseMemory(MyAddonMemories.CAMPFIRE.get());
        ErrandUtils.finish(mob, WarmthNeedSatisfier.WARM_UP);
    }
}
```

The memory and the behavior are added to the Xoonglin brain through the
[brain event](#extending-the-xoonglin-brain):

```java
@EventBusSubscriber(modid = MyAddon.MOD_ID)
public class MyAddonBrainEvents {
    @SubscribeEvent
    public static void registerMemories(XoonglinBrainEvent.RegisterMemories event) {
        event.addMemory(MyAddonMemories.CAMPFIRE.get());
    }

    @SubscribeEvent
    public static void addBehaviors(XoonglinBrainEvent.AddBehaviors event) {
        event.addBehavior(Activity.INVESTIGATE, 1, new WarmUpBehavior());
    }
}
```

`MyAddonMemories.CAMPFIRE` is a regular memory type, registered with a `DeferredRegister` on
`Registries.MEMORY_MODULE_TYPE`. A datapack then defines the need in
`data/myaddon/cft/need/campfire_need.json`, and a social class lists it as `myaddon:campfire_need`:

```json
{
  "type": "myaddon:warmth",
  "damage": 0.0,
  "damage_threshold": 0.0,
  "provided_happiness": 3.0,
  "satisfaction_threshold": 0.4,
  "frequency": 1.0,
  "search_radius": 32
}
```

## Adding a job type

A job type is a `Job`, read from JSON, which decides when there's work to do, and a behavior that
does it.

**The `Job`** passes the fields every job has (`required_needs`, `min_happiness`,
`available_to_babies`, `available_to_adults`) to its constructor, so declare those in your codec.
The `schedule` field is added to every job's codec by CFT. Implement:

- `tick(xoonglin, state)`: called every tick while the job isn't paused. It keeps the daily quota
  in `state` (a `JobState` saved with the Xoonglin: worked ticks today, days in a row...), and
  sets its work memory while there's work to do (e.g. the quota isn't met and `canWork(xoonglin)`
  holds), erasing it otherwise.
- `getWorkMemory()`: that memory. CFT checks it to know the Xoonglin is working, and uses the
  `WORK` activity then.
- `eraseMemories(xoonglin)`: erase the memories `tick` sets. CFT calls it instead of `tick` while
  the job is paused: while the Xoonglin attends a ritual, outside its working hours, and while it
  has [errands](#errands). The job's behavior stops then.
- `getDisplayInfo(xoonglin, state)`: the job tab: a status and a list of entries (see below).
- `jobType()`: your registered codec.
- `getRequiredStructureType()`: _(optional)_ the structure type the job works at. While the
  Xoonglin has none, set the `CftMemoryModuleType.STRUCTURE_NEEDED` memory to the type's id: CFT
  finds one with room and claims it in the Xoonglin's free time. Then
  `xoonglin.getAssignedStructurePos(type)` gives its key block.

**The behavior** extends `JobBehavior<YourJob>`, which does the plumbing every job behavior shares:

- It starts when the job's work memory is set, and stops once it's gone or the Xoonglin leaves the
  `WORK` activity, for whatever reason.
- `getJob(xoonglin)` gives the Xoonglin's job as your type.
- It shows the step of the work it's on in the job tab, through `workStep()`.

Implement `tickWork` (and override `start`, `stopWork`, `checkExtraStartConditions` and
`canKeepWorking` as needed):

```java
public class KeepBeesBehavior extends JobBehavior<BeekeeperJob> {
    private enum State {
        TRAVELING(WorkStep.GOING_TO_WORK),
        HARVESTING(new WorkStep("gui.myaddon.work_step.harvesting_honey"));

        private final WorkStep step;
        State(WorkStep step) { this.step = step; }
    }

    private State state;

    public KeepBeesBehavior() {
        super(MyAddonMemories.MUST_KEEP_BEES.get(), BeekeeperJob.class, 2400);
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity mob, long gameTime) {
        state = State.TRAVELING;
    }

    @Override
    protected WorkStep workStep() {
        return state != null ? state.step : null;
    }

    @Override
    protected void tickWork(ServerLevel level, XoonglinEntity mob, long gameTime) {
        BeekeeperJob job = getJob(mob);
        if (job == null) return;
        // Walk to the apiary, then harvest its hives...
    }
}
```

Add the behavior to the `WORK` activity with the brain event, and register the job's work memory
there too.

**The job tab** shows what `getDisplayInfo` returns: `new JobInfoData(status, entries)`.

- The status is a `JobStatus`: a lang key and a color. Use the shared ones where they fit:
  `noStructureStatus()` and `cantWorkStatus(xoonglin)` (from `Job`, which say what's missing),
  `JobUtil.workingStatus(xoonglin)` (which shows the behavior's work step) and
  `JobStatus.RESTING` (the quota is met). Define your own with `JobStatus.attention(key)` for
  problems, or `JobStatus.active(key)` for ways of working.
- The entries are `JobDisplayEntry.progress(...)` (a bar, like today's work:
  `JobUtil.formatWorkTime` formats it), `JobDisplayEntry.item(...)` (an item icon) or
  `JobDisplayEntry.text(...)`.
- CFT shows its own statuses over yours when it applies: sleeping, off duty, and paused for
  errands.

Look at `SmelterJob` and `SmeltBehavior` for a small, complete job.

## Adding a structure type

Structure types detect structures in the world when the leader uses the staff on their key block.
A type extends `StructureType` (whose constructor takes the `key_block`/`key_block_tag`,
`max_users`, `requires_container` and `priority` fields) and implements `detect(keyBlockPos,
level, leaderId)`, returning a `StructureDetectionResult` with the detected `Structure` or the
reason it failed. Register its codec on `STRUCTURE_TYPE_CODEC_KEY`. The built-in types in
`com.hyperbaton.cft.structure.type` show how detection is done.

## Errands

An errand takes a Xoonglin away from its work for something that can't wait, like fetching what a
need requires. Errands are kept in the `ERRANDS_PAUSING_WORK` memory, as a set of ids, and
`ErrandUtils` handles them:

- `ErrandUtils.start(xoonglin, id)`: start an errand. While the Xoonglin has any, its job is
  paused, it goes to the `INVESTIGATE` activity (even in working hours, but not while asleep),
  and the job tab shows "Paused" with the errands' lang entries (`errand.<namespace>.<path>`).
- `ErrandUtils.finish(xoonglin, id)`: finish it, whether it succeeded or not. Once it has none
  left, the job resumes.
- `ErrandUtils.startUnlessCoolingDown(xoonglin, id, cooldownMemory)`: start it only if the given
  memory isn't set. Set that memory with an expiry when the behavior gives up (e.g. the target
  can't be reached), so the Xoonglin goes back to work for a while before retrying.

Every errand has its own id, so one finishing doesn't resume the job while another is still
pending. Start an errand only when there's something the Xoonglin can do about it right away, and
make sure every way your behavior can end finishes it: otherwise the job stays paused. Errands
aren't saved with the world, so after a reload they're only back once whatever started them
starts them again.

## Extending the Xoonglin brain

`XoonglinBrainEvent` is posted on `NeoForge.EVENT_BUS` while a Xoonglin brain is built, on both
the server and the client:

- `XoonglinBrainEvent.RegisterMemories`: posted once, the first time a brain is built. Add the
  memories and sensors your behaviors use with `addMemory` and `addSensor`. The brain ignores
  memories it wasn't built with, so register every memory you set, including your jobs' work
  memories.
- `XoonglinBrainEvent.AddBehaviors`: posted for every Xoonglin brain. Add behaviors with
  `addBehavior(activity, priority, behavior)`, creating a new behavior every time: behaviors keep
  the state of the Xoonglin running them. Use CFT's activities (`WORK`, `INVESTIGATE`, `IDLE`,
  `REST`): CFT picks the activity, so behaviors in any other activity never run.

Keep in mind that Minecraft doesn't stop a behavior when its activity stops being the running one:
it only stops once its own `canStillUse` says so, or its duration is over. Have `canStillUse` check
the activity or its memories, as `JobBehavior` does. And since the behaviors of an activity run at
the same time, avoid two of them steering the Xoonglin's navigation at once.

## Lang entries

| Text | Lang key |
|---|---|
| A need's name | the need's id, e.g. `myaddon:campfire_need` |
| A need's description, in its tooltip | `need.<namespace>.<path>.tooltip` of the need's id |
| A job's name | `job.<namespace>.<path>` of the job file's id, e.g. `job.myaddon.beekeeper_job` |
| A structure type's name | the structure type's id |
| A work step | the key of its `WorkStep` (`gui.cft.work_step.<name>` with `WorkStep.of(name)`) |
| An errand | `errand.<namespace>.<path>` of its id |
| A job status | the key of its `JobStatus` |
