# Needs

All needs have some basic fields: `id`, `type`, `damage`, `damage_threshold`, `satisfaction_threshold`,
`frequency` and `provided_happiness`. Then depending on the `type`, they might have extra
fields and they will work differently.

All needs are checked every second for all Xoonglins. They have an internal value of
satisfaction that goes from 0 to 1. If it's above the `satisfaction_threshold`, it is
considered satisfied. In that case, happiness is increased and satisfaction is reduced.
If it is unsatisfied, Xoonglin will try to initiate
some action for satisfying the need. If it fails, happiness will decrease.

## Goods Need

A Xoonglin needs to consume some item or block for this need to be satisfied. They will
try to get the goods from the container within their home. When resupplying, the Xoonglin
will take up to the `hoarding` amount from the container so it doesn't need to resupply
as often.

??? example "Sample goods need file"

    ```json
    {
      "type": "cft:goods",
      "damage": 0.5,
      "damage_threshold": 0.5,
      "provided_happiness": 10,
      "satisfaction_threshold": 0.75,
      "item": {
        "item": "minecraft:bread"
      },
      "frequency": 0.5,
      "quantity": 2,
      "hoarding": 10
    }
    ```

    - `type`: Must be `"cft:goods"` to indicate this is a goods need.
    - `damage`: Amount of damage per second if the need is unsatisfied.
    - `damage_threshold`: The Xoonglin will receive damage if satisfaction falls below this level.
    - `provided_happiness`: Happiness provided per Minecraft day (20 real minutes) if the need
    is satisfied.
    - `satisfaction_threshold`: If satisfaction is above this level, the need is considered
    _satisfied_.
    - `item`: The needed item, in Ingredient format. It can be just a reference to some item or have more complex structure,
    like checking NBT tags.
    - `frequency`: In Minecraft days, how long it takes for the satisfaction of this need to
    go from 1 to 0.
    - `hidden`: _(Optional, default: false)_ Whether this need should be hidden from interfaces.
    - `bonus`: _(Optional, default: false)_ If true, the need only adds happiness when satisfied and never
    subtracts it when unsatisfied — useful for festivals or luxuries.
    - `quantity`: How many items of the specified type are consumed each time the need is satisfied.
    - `hoarding`: _(Optional)_ How many items the Xoonglin will take from the container when
    resupplying. If omitted or set to 0, defaults to `quantity`. Setting this higher than
    `quantity` means the Xoonglin will stock up and won't need to visit the container as often.

## Home Need

A Xoonglin needs a home of a specific structure type. The home need references a house
structure type (defined in the Structures section), which specifies the building rules.

Once built, right clicking with the **Leader Staff** on the door of a house will check
if it's valid. A message will appear in chat informing if it is or not. The house is checked
again every time the need is checked (its `frequency`); if it no longer passes, the Xoonglin
loses its home and looks for another one (see [Keeping structures up to date](structures.md#keeping-structures-up-to-date)).

??? example "Sample home need file"

    ```json
    {
      "type": "cft:home",
      "damage": 0.0,
      "damage_threshold": 0.0,
      "provided_happiness": 8,
      "satisfaction_threshold": 0.95,
      "frequency": 1.0,
      "required_structure": "cft:settler_house"
    }
    ```

    For the common fields, look at the goods need example. The specific field for home needs is:

    - `required_structure`: A reference to a house structure type ID (e.g. `"cft:settler_house"`).
      The Xoonglin's home must match this structure type to satisfy the need.

## Equipment Need

A Xoonglin needs to wear a specific item in an equipment slot. The Xoonglin will look for
the item in its inventory and equip it automatically. If the item is not available, the
Xoonglin will try to get it from the container in its home. Equipment items take damage
over time and will eventually need to be replaced.

??? example "Sample equipment need file"

    ```json
    {
      "type": "cft:equipment",
      "damage": 0.0,
      "damage_threshold": 0.0,
      "provided_happiness": 0.0,
      "satisfaction_threshold": 0.5,
      "item": {
        "item": "minecraft:iron_sword"
      },
      "frequency": 7,
      "hidden": true,
      "slot": "mainhand"
    }
    ```

    For the common fields, look at the goods need example. The specific fields for equipment needs are:

    - `item`: The equipment item, in Ingredient format.
    - `slot`: _(Optional, default: "mainhand")_ The equipment slot where the item should be worn.
      Valid values: `"mainhand"`, `"offhand"`, `"head"`, `"chest"`, `"legs"`, `"feet"`.

## Structure Need

A Xoonglin needs access to a specific structure type nearby. This is used when a Xoonglin
requires a workplace or facility (e.g. a smithy) without it being their home.

??? example "Sample structure need file"

    ```json
    {
      "type": "cft:structure",
      "damage": 0.0,
      "damage_threshold": 0.0,
      "provided_happiness": 5.0,
      "satisfaction_threshold": 0.9,
      "frequency": 1.0,
      "hidden": false,
      "required_structure": "cft:smithy",
      "requires_usage": true,
      "search_radius": 64
    }
    ```

    For the common fields, look at the goods need example. The specific fields for structure needs are:

    - `required_structure`: A reference to a structure type ID that the Xoonglin needs access to.
    - `requires_usage`: _(Optional, default: false)_ If true, the Xoonglin must actively visit
      and interact with the structure; if false, the structure just needs to exist nearby.
    - `search_radius`: _(Optional, default: 64)_ How far (in blocks) the Xoonglin will search
      for the required structure.
    - `requires_running`: _(Optional, default: false)_ If true, only running structures are visited:
      those where one of their workers is working right now, like a tavern whose keeper is on shift.
      See the [structure need](#structure-need) for what counts as a worker.
    - `running_work_steps`: _(Optional)_ With `requires_running`, the work steps the worker must be on.
      They are the steps shown in the job tab, named after their lang entries (`gui.cft.work_step.<step>`).

## Altitude Need

The Xoonglin needs to live in some altitude range (given by Y coordinate).

??? example "Sample altitude need file"

    ```json
    {
      "type": "cft:altitude",
      "damage": 0.4,
      "damage_threshold": 0.5,
      "provided_happiness": 5,
      "satisfaction_threshold": 0.75,
      "frequency": 0.1,
      "min_altitude": 100,
      "max_altitude": 300
    }
    ```
    Apart from the common fields, this need includes an altitude range:
    - `min_altitude`: The minimum altitude the Xoonglin must be at any moment for the need to be satisfied.
    - `max_altitude`: The altitude below which the Xoonglin must be at any moment for the need to be satisfied.

## Biome Need

The Xoonglin needs to live in a given biome (or a set of biomes).

??? example "Sample biome need file"

    ```json
    {
      "type": "cft:biome",
      "damage": 0.4,
      "damage_threshold": 0.5,
      "provided_happiness": 5,
      "satisfaction_threshold": 0.75,
      "frequency": 0.1,
      "biomes": [
        "minecraft:windswept_hills",
        "minecraft:windswept_gravelly_hills",
        "minecraft:windswept_forest",
        "minecraft:windswept_savanna",
        "minecraft:meadow",
        "minecraft:cherry_grove",
        "minecraft:grove",
        "minecraft:snowy_slopes",
        "minecraft:frozen_peaks",
        "minecraft:jagged_peaks",
        "minecraft:stony_peaks"
      ]
    }
    ```
    Apart from the common fields, this need includes the biomes:
    - `biomes`: The biomes the Xoonglin must be in: a biome, a biome tag (e.g.
      `"#minecraft:is_mountain"`) or a list of them (see [Game content](index.md#game-content)).

## Fluid Need

It's similar to the Goods Need, but in this case the goods are fluids that are taken from a container within the home
of the Xoonglin. Enough fluid must be present there; if it is, the Xoonglin will go to it to retrieve the fluid and
satisfy the need.

??? example "Sample fluid need file"

    ```json
    {
      "type": "cft:fluid",
      "damage": 0.5,
      "damage_threshold": 0.6,
      "provided_happiness": 2.0,
      "satisfaction_threshold": 0.8,
      "frequency": 1.0,
      "fluid_stack": {
        "FluidName": "minecraft:water",
        "Amount": 1000
      }
    }
    ```
    Apart from the common fields, this need includes a fluid stack object. Yes, it's in PascalCase because it uses NeoForge
    parsing method for FluidStack.
    - `fluid_stack`: A FluidStack object that contains the reference of the fluid and the amount in millibuckets.

## Energy Need

Works very similar to the Fluid Needs, but in this case the product consumed is just NeoForge Energy. It can be taken from
any block within the Xoonglin's home that implements the IEnergyStorage interface. The block must contain enough energy
within itself and also be able to manage enough throughput: If the need requires more energy at once, the Xoonglin may
not be able to extract it. This can be tuned by balancing amount and frequency.

??? example "Sample energy need file"

    ```json
    {
      "type": "cft:energy",
      "damage": 1.0,
      "damage_threshold": 0.2,
      "provided_happiness": 1.0,
      "satisfaction_threshold": 0.8,
      "frequency": 1.0,
      "energy_amount": 50
    }
    ```
    The only specific field for this need is the energy amount.
    - `energy_amount`: The amount that must be consumed in one go to satisfy the need, in NeoForge Energy units.

## Social Need

The Xoonglin needs companions of a specific social class nearby.

??? example "Sample social need file"

    ```json
    {
      "type": "cft:social",
      "damage": 0.5,
      "damage_threshold": 0.2,
      "provided_happiness": 1.0,
      "satisfaction_threshold": 0.8,
      "frequency": 0.2,
      "hidden": false,
      "classes": ["cft:settler"],
      "min_count": 2,
      "max_count": 30,
      "radius": 50
    }
    ```
    - `classes`: A list of social class IDs. Xoonglins of these classes count as companions.
    - `min_count`: Minimum number of companions needed.
    - `max_count`: Maximum number of companions that count toward satisfaction.
    - `radius`: How far to search for companions.

## Pet Need

The Xoonglin needs some mobs of specific types to be around (or to be absent: this
need can also be used for limiting the presence of some mobs, e.g. hostile ones).

??? example "Sample pet need file"

    ```json
    {
      "type": "cft:pet",
      "damage": 0.5,
      "damage_threshold": 0.2,
      "provided_happiness": 3.0,
      "satisfaction_threshold": 0.8,
      "frequency": 1.0,
      "hidden": false,
      "entity_types": ["minecraft:cat"],
      "min_count": 1,
      "max_count": 5,
      "radius": 16
    }
    ```
    - `entity_types`: The entities that count as pets: entity types, entity type tags or a list of
      them (see [Game content](index.md#game-content)).
    - `min_count`: Minimum number of matching pets needed.
    - `max_count`: Maximum number of pets that count toward satisfaction.
    - `radius`: How far to search for pets.

## Lighting Need

The Xoonglin needs a certain light level around them.

??? example "Sample lighting need file"

    ```json
    {
      "type": "cft:lighting",
      "damage": 0.3,
      "damage_threshold": 0.5,
      "provided_happiness": 2.0,
      "satisfaction_threshold": 0.75,
      "frequency": 0.5,
      "hidden": true,
      "min_light": 8,
      "radius": 3
    }
    ```
    - `min_light`: Minimum light level (0-15) required for the need to be satisfied.
    - `radius`: _(Optional, default: 0)_ Sampling radius around the Xoonglin's position.
      If 0, only the block at the Xoonglin's position is checked.

## Decoration Need

The Xoonglin needs specific decorative blocks placed near their home.

??? example "Sample decoration need file"

    ```json
    {
      "type": "cft:decoration",
      "damage": 0.2,
      "damage_threshold": 0.5,
      "provided_happiness": 5.0,
      "satisfaction_threshold": 0.75,
      "frequency": 1.0,
      "block": "#minecraft:flowers",
      "min_count": 6,
      "radius": 8,
      "min_spread": 0.3
    }
    ```
    - `block`: The decorative blocks to look for: a block, a block tag or a list of them
      (see [Game content](index.md#game-content)).
    - `min_count`: Minimum number of matching blocks required.
    - `radius`: Search radius around the home entrance.
    - `min_spread`: _(Optional, default: 0.0)_ Minimum spatial spread of the blocks (0 to 1).

## Ritual Need

The Xoonglin needs a ritual to take place nearby. Despite the name, this doesn't have to
be anything religious: any kind of ceremony, event or performance can be configured with
this need type: a communal meal, a market day, a concert, a festival... Rituals are
performed by Xoonglins with the **Officiant job** (see the Jobs section), and the need is
satisfied when a ritual with a matching `ritual_id` completes within the given radius.

??? example "Sample ritual need file"

    ```json
    {
      "type": "cft:ritual",
      "damage": 0.0,
      "damage_threshold": 0.0,
      "provided_happiness": 5.0,
      "satisfaction_threshold": 0.9,
      "frequency": 1.0,
      "hidden": false,
      "ritual_id": "cft:communion",
      "radius": 32,
      "requires_presence": true
    }
    ```

    For the common fields, look at the goods need example. The specific fields for ritual needs are:

    - `ritual_id`: Identifier of the ritual that satisfies this need. It must match the
      `ritual_id` of an officiant job for the ritual to ever be performed.
    - `radius`: _(Optional, default: 16)_ How close (in blocks) the ritual must take place
      for this Xoonglin to benefit from it.
    - `requires_presence`: _(Optional, default: false)_ If true, the Xoonglin must attend the
      ritual in person, from beginning to end, standing within the ritual radius. If false,
      it is enough that the ritual completes nearby, wherever the Xoonglin happens to be.

## Reading Need

The Xoonglin wants a book from the world's shared **roster** of writer-produced books
(see the Writer job). It works exactly like a Goods Need: the Xoonglin fetches a
matching book from its home container into its own inventory, then consumes it to satisfy the need. Which book is "wanted" is picked automatically each check —
a stable pick that shifts over time as new books are written — rather than assigned once
and remembered, so there's nothing to configure about which title.

??? example "Sample reading need file"

    ```json
    {
      "type": "cft:reading",
      "damage": 0.0,
      "damage_threshold": 0.0,
      "provided_happiness": 60.0,
      "satisfaction_threshold": 0.5,
      "frequency": 14
    }
    ```

    For the common fields, look at the goods need example. This need has no extra fields —
    what's "required" comes entirely from the current roster of book titles.

## Sleep Need

The Xoonglin needs to sleep in a bed inside its home, so its home needs a free bed for each
Xoonglin living there that has this need (a house type can allow or require beds through its
`interiorBlocks`).

While the need is unsatisfied, the Xoonglin will go to bed as soon as it's time to sleep: during
the rest time of its [schedule](schedules.md) or, if it has no schedule, at night. It then sleeps
until that time is over (until sunrise, without a schedule), even though the need is satisfied as
soon as it falls asleep.

Unlike other needs, it doesn't wear off while the Xoonglin sleeps, so it always wakes up fully
rested. Once awake, satisfaction wears off over `frequency` days. If it drops below
`satisfaction_threshold` during the day, the Xoonglin is unhappy until it can sleep again; if it
drops during rest time, it just goes to bed. For example, with `frequency` 1, a threshold of 0.3
lasts about 17 hours after waking up.

??? example "Sample sleep need file"

    ```json
    {
      "type": "cft:sleep",
      "damage": 0.0,
      "damage_threshold": 0.0,
      "provided_happiness": 3.0,
      "satisfaction_threshold": 0.3,
      "frequency": 1.0
    }
    ```

    For the common fields, look at the goods need example. This need has no extra fields.

## Visit Need

The Xoonglin wants to spend some of its free time at a structure of some type: a tavern, a
market, a plaza... When the need is unsatisfied, it walks to the nearest structure of that type
(of its leader, and within `search_radius` of its home) and it is satisfied as soon as it gets
there. Then it stays for a while, strolling around the structure, before going back to what it
was doing. How often it goes there is just the need's `frequency`.

Visits only happen in free time: during leisure hours if the Xoonglin has a
[schedule](schedules.md), or whenever it isn't working otherwise.

??? example "Sample visit need file"

    ```json
    {
      "type": "cft:visit",
      "damage": 0.0,
      "damage_threshold": 0.0,
      "provided_happiness": 4.0,
      "satisfaction_threshold": 0.5,
      "frequency": 1.0,
      "required_structure": "cft:village_square",
      "search_radius": 64,
      "stay_duration": 1200
    }
    ```

    For the common fields, look at the goods need example.

    - `required_structure`: A reference to the structure type to visit.
    - `search_radius`: _(Optional, default: 64)_ How far from the Xoonglin's home the structure
      can be (Manhattan distance).
    - `stay_duration`: _(Optional, default: 1200)_ How long the Xoonglin stays once it arrives, in
      ticks (20 ticks = 1 second).
    - `consumes`: _(Optional)_ A list of goods taken from the structure's containers when the
      Xoonglin arrives, like a drink at the tavern, in the same format as job inputs
      (`{"item": ..., "quantity": ...}`). Only structures holding all of them are visited, so a
      tavern that runs out stops attracting visitors until it's restocked.
    - `use_supplies`: _(Optional, default: false)_ If true, while the Xoonglin is visiting, it can
      satisfy its goods, fluid and energy needs from the structure's containers, just as it does
      with the ones in its home (and preferring the structure's). From there it only takes what
      it needs right now: `hoarding` only applies to its home. Leave it off for structures whose
      containers hold goods for other purposes, like a job's inputs or a ritual's ingredients.
    - `requires_running`: _(Optional, default: false)_ If true, only running structures are visited:
      those where one of their workers is working right now, like a tavern whose keeper is on shift.
      See the [structure need](#structure-need) for what counts as a worker.
    - `running_work_steps`: _(Optional)_ With `requires_running`, the work steps the worker must be on,
      as in the structure need.

??? example "Sample tavern visit need"

    The mod ships this need together with a sample `cft:tavern` structure: an enclosed wooden
    building whose key block is a barrel, which can also hold the tavern's stock. The tavern is only
    open while a tavern keeper is working there: the sample `cft:tavern_keeper_job` is a trader at the
    tavern, working from 14:00 to 22:00.

    ```json
    {
      "type": "cft:visit",
      "damage": 0.0,
      "damage_threshold": 0.0,
      "provided_happiness": 6.0,
      "satisfaction_threshold": 0.5,
      "frequency": 1.0,
      "required_structure": "cft:tavern",
      "consumes": [
        { "item": { "item": "minecraft:honey_bottle" }, "quantity": 1 }
      ],
      "use_supplies": true,
      "requires_running": true
    }
    ```

## Socialize Need

The Xoonglin wants to spend some of its free time with another Xoonglin. Unlike the social need,
which only checks who lives nearby, this one makes two Xoonglins actually meet. For now,
socializing means having a conversation: the Xoonglin walks to the nearest one that is free
(awake, not working, not busy with its own needs), of the same leader and of an accepted class.
They face each other and talk for a while, and then the socialize needs of both of them are
satisfied, as long as each one accepts the other's class. The partner doesn't need to have a
socialize need itself.

??? example "Sample socialize need file"

    ```json
    {
      "type": "cft:socialize",
      "damage": 0.0,
      "damage_threshold": 0.0,
      "provided_happiness": 2.0,
      "satisfaction_threshold": 0.5,
      "frequency": 0.5,
      "classes": [],
      "radius": 24,
      "duration": 160
    }
    ```

    For the common fields, look at the goods need example.

    - `classes`: _(Optional)_ The social classes of the Xoonglins it is willing to talk to. If empty
      or omitted, anyone will do.
    - `radius`: _(Optional, default: 24)_ How far away it looks for someone to talk to.
    - `duration`: _(Optional, default: 160)_ How long a conversation lasts once they are
      together, in ticks (20 ticks = 1 second).

## Hearing Need

The Xoonglin wants to hear some sounds around it now and then: music, the bustle of a busy
street... It can also be the opposite, a Xoonglin that wants quiet. The need is satisfied while
the Xoonglin has heard the right number of those sounds recently, wherever it is; it doesn't go
looking for them.

A Xoonglin hears two kinds of sounds, and a need can list both:

- **Sounds** played by the server, as far as a player would hear them: 16 blocks for most, and
  further for loud ones (a bell reaches 32 blocks), up to 64 blocks. Type `/playsound ` in game
  and press Tab to browse them. Some sounds are made only on the player's screen and can't be
  heard: jukebox music, blocks breaking, campfires, rain.
- **Game events**, the vibrations the Warden and sculk sensors react to: steps, blocks placed or
  broken, doors, containers, eating, explosions, note blocks, jukeboxes playing (once per second),
  goat horns... Most reach 16 blocks, and jukeboxes 10. The Minecraft wiki's "Game event" page
  lists them.

It never hears its own sounds. Some things make both a sound and a game event (a note block
plays `block.note_block.harp` and sends `note_block_play`); listing both counts them twice.

??? example "Sample hearing need file"

    ```json
    {
      "type": "cft:hearing",
      "damage": 0.0,
      "damage_threshold": 0.0,
      "provided_happiness": 4.0,
      "satisfaction_threshold": 0.5,
      "frequency": 1.0,
      "bonus": true,
      "game_events": ["minecraft:jukebox_play", "minecraft:note_block_play", "minecraft:instrument_play"],
      "sounds": ["minecraft:block.bell.use"],
      "min_events": 5,
      "window": 60
    }
    ```

    For the common fields, look at the goods need example.

    - `sounds`: _(Optional)_ The sounds it wants to hear: a sound, a sound tag or a list of them
      (see [Game content](index.md#game-content)).
    - `game_events`: _(Optional)_ The game events it wants to hear: a game event, a game event
      tag (e.g. `"#minecraft:vibrations"`) or a list of them (see [Game content](index.md#game-content)).
    - `min_events`: _(Optional, default: 1)_ How many of them it must have heard within the
      `window` for the need to be satisfied.
    - `max_events`: _(Optional)_ If given, the need is not satisfied either if it heard more than
      this. With `min_events` at 0, it makes a need for quiet.
    - `window`: _(Optional, default: 60)_ How far back it remembers what it heard, in seconds.
