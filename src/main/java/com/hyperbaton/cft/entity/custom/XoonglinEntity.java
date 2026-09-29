package com.hyperbaton.cft.entity.custom;

import com.google.common.collect.ImmutableList;
import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.job.JobState;
import com.hyperbaton.cft.network.ClassChangeNotificationPacket;
import com.hyperbaton.cft.entity.ai.schedule.ScheduleUtils;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfierMapper;
import com.hyperbaton.cft.need.NeedUtils;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.ai.XoonglinAi;
import com.hyperbaton.cft.entity.ai.activity.CftActivities;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.socialclass.SocialClassUpdate;
import com.hyperbaton.cft.socialclass.SocialStructureHelper;
import com.hyperbaton.cft.sound.CftSounds;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.home.HouseStructure;
import com.hyperbaton.cft.world.RitualsData;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;

public class XoonglinEntity extends AgeableMob implements InventoryCarrier {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int DELAY_BETWEEN_NEEDS_CHECKS = 20;
    private static final EntityDimensions XOONGLIN_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.4F);
    private static final EntityDimensions HUMANOID_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.95F);
    private static final int FULL_HEAL_TICKS = 24000;
    public static final EntityDataAccessor<String> SOCIAL_CLASS_NAME = SynchedEntityData.defineId(XoonglinEntity.class, EntityDataSerializers.STRING);
    /** Synced so the client can show the need indicator only to the Xoonglin's own leader. */
    public static final EntityDataAccessor<Optional<UUID>> LEADER_UUID = SynchedEntityData.defineId(XoonglinEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** One of the ALERT_* levels, describing the worst visible need. */
    public static final EntityDataAccessor<Byte> NEED_ALERT = SynchedEntityData.defineId(XoonglinEntity.class, EntityDataSerializers.BYTE);
    /** Item id used as icon for the worst visible need, or empty if there is none. */
    public static final EntityDataAccessor<String> NEED_ALERT_ICON = SynchedEntityData.defineId(XoonglinEntity.class, EntityDataSerializers.STRING);

    public static final byte ALERT_NONE = 0;
    /** Some need is below its satisfaction threshold. */
    public static final byte ALERT_UNSATISFIED = 1;
    /** Some harmful need is below its damage threshold, so the Xoonglin is getting hurt. */
    public static final byte ALERT_CRITICAL = 2;

    public XoonglinEntity(EntityType<? extends AgeableMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        ((GroundPathNavigation) this.getNavigation()).setCanPassDoors(true);
        ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        if (!pLevel.isClientSide && !this.hasCustomName()) {
            this.setCustomName(Component.literal(XoonglinNameGenerator.generateName()));
        }
        // The constructor sizes entities after their EntityType, and vanilla only recomputes
        // the size when the pose, baby flag or scale change, which never happens for adults.
        this.refreshDimensions();
    }

    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    private UUID leaderId;
    private final SimpleContainer inventory = new SimpleContainer(27);
    private final List<com.hyperbaton.cft.job.data.TradeOffer> tradeOffers = new ArrayList<>();

    private HouseStructure home;

    private List<NeedSatisfier<? extends Need>> needs;

    private SocialClass socialClass;

    private double happiness = 0.0;

    private ResourceLocation jobId;
    private final JobState jobState = new JobState();

    private final Map<String, BlockPos> assignedStructurePositions = new HashMap<>();

    private int satisfyNeedsDelay = DELAY_BETWEEN_NEEDS_CHECKS;
    private int matingDelay = CftConfig.XOONGLIN_MATING_COOLDOWN.get();

    private static final String KEY_LEADER_ID = "leaderId";
    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_HOME = "home";
    private static final String KEY_NEEDS = "needs";
    private static final String KEY_SOCIAL_CLASS = "socialClass";
    private static final String KEY_HAPPINESS = "happiness";
    public static final String KEY_JOB_ID = "jobId";
    public static final String KEY_JOB_STATE = "jobState";
    private static final String KEY_ASSIGNED_STRUCTURES = "assignedStructures";
    private static final String KEY_TRADE_OFFERS = "tradeOffers";

    @Override
    public void tick() {
        super.tick();

        if (satisfyNeedsDelay > 0) {
            satisfyNeedsDelay--;
        } else if (needs == null) {
            satisfyNeedsDelay = DELAY_BETWEEN_NEEDS_CHECKS;
        } else {
            for (NeedSatisfier currentNeed : needs) {
                Need need = currentNeed.getNeed();
                if (currentNeed.getSatisfaction() < need.getSatisfactionThreshold()) {
                    currentNeed.satisfy(this);
                } else {
                    currentNeed.unsatisfy(need.getFrequency(), this);
                    increaseHappiness(need.getProvidedHappiness(), need.getFrequency());
                }
                currentNeed.setSatisfied(!(currentNeed.getSatisfaction() < need.getSatisfactionThreshold()));
            }
            if (allDamagingNeedsSatisfied()) {
                float healAmount = getMaxHealth() * DELAY_BETWEEN_NEEDS_CHECKS / (float) FULL_HEAL_TICKS;
                heal(healAmount);
            }
            updateNeedAlert();
            checkSocialClass();
            satisfyNeedsDelay = DELAY_BETWEEN_NEEDS_CHECKS;
        }

        if (this.level().isClientSide()) {
            setupAnimationStates();
        }

        if (!level().isClientSide && matingDelay > 0) {
            matingDelay--;
        }

        if (!level().isClientSide && jobId != null) {
            Job job = CftRegistry.JOBS.get(jobId);
            if (job != null) {
                if (getBrain().hasMemoryValue(CftMemoryModuleType.MUST_ATTEND_RITUAL.get())) {
                    // Attending a ritual preempts the day job
                    job.eraseMemories(this);
                } else if (ScheduleUtils.isOffDuty(this)) {
                    // Outside the working hours of its schedule
                    job.eraseMemories(this);
                } else if (isFetchingSupplies()) {
                    // Its needs come first: the job resumes once it has fetched what it needs
                    job.eraseMemories(this);
                } else {
                    job.tick(this, jobState);
                }
            } else {
                LOGGER.warn("Xoonglin {} has invalid job ID '{}', clearing it", getName().getString(), jobId);
                jobId = null;
                jobState.reset();
            }
        }

        if (!this.level().isClientSide && CftConfig.KEEP_XOONGLINS_LOADED.get()) {
            ChunkPos chunkPos = new ChunkPos(this.blockPosition());
            ((ServerLevel) this.level()).getChunkSource().addRegionTicket(
                    CftRegistry.XOONGLIN_CHUNK_TICKET,
                    chunkPos,
                    1,
                    this.getUUID()
            );
        }
    }

    @Override
    protected void customServerAiStep() {
        Brain<XoonglinEntity> brain = this.getBrain();

        ScheduleUtils.updateBrainSchedule(this);
        brain.tick((ServerLevel) level(), this);
        updateActivity();
    }

    /**
     * Picks the activity: attending a ritual, then working, resting, mating, errands and free time,
     * in that order. Fetching supplies still comes before work, because it pauses the job (see
     * {@link #tick()}), so the job's memories are gone by then.
     */
    private void updateActivity() {
        boolean resting = shouldRest();

        if (this.getBrain().hasMemoryValue(CftMemoryModuleType.MUST_ATTEND_RITUAL.get())) {
            setActivity(Activity.INVESTIGATE);
        } else if (isWorkingAtJob()) {
            setActivity(Activity.WORK);
        } else if (resting) {
            setActivity(hasSuppliesToFetch() ? Activity.INVESTIGATE : Activity.REST);
        } else if (isReadyToMate()) {
            setActivity(CftActivities.MATE.get());
        } else if (isWorkInterrupted()) {
            setActivity(Activity.INVESTIGATE);
        } else {
            setActivity(Activity.IDLE);
        }
    }

    private void setActivity(Activity activity) {
        this.getBrain().setActiveActivityToFirstValid(ImmutableList.of(activity, Activity.IDLE));
    }

    private boolean isReadyToMate() {
        Brain<XoonglinEntity> brain = this.getBrain();
        return brain.getMemory(CftMemoryModuleType.CAN_MATE.get()).orElse(false)
                && brain.hasMemoryValue(CftMemoryModuleType.MATING_CANDIDATE.get());
    }

    /**
     * Whether its job has it working right now: jobs set their MUST_* memory while there's work to
     * do, within working hours and when the Xoonglin can work.
     */
    public boolean isWorkingAtJob() {
        Brain<XoonglinEntity> brain = this.getBrain();
        return brain.hasMemoryValue(CftMemoryModuleType.MUST_WORK_AT_HOME.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_GATHER.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_GUARD.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_FARM.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_HAUL.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_BUILD.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_PERFORM_RITUAL.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_CRAFT.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_SMELT.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_CHOP.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_FISH.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_HEAL.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_BLESS.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_MINE.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_ENCHANT.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_RANCH.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_WRITE.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_SCRIBE.get())
                || brain.hasMemoryValue(CftMemoryModuleType.MUST_TRADE.get());
    }

    /**
     * Whether it has something to do outside its job's behaviors
     */
    private boolean isWorkInterrupted() {
        Brain<XoonglinEntity> brain = this.getBrain();
        return brain.hasMemoryValue(CftMemoryModuleType.HOME_NEEDED.get())
                || isFetchingSupplies()
                || (brain.hasMemoryValue(CftMemoryModuleType.STRUCTURE_NEEDED.get())
                && !brain.hasMemoryValue(CftMemoryModuleType.STRUCTURE_SEARCH_COOLDOWN.get()));
    }

    /** Whether it has goods, fluid or energy to fetch from a container it knows of. */
    private boolean isFetchingSupplies() {
        return hasSuppliesToFetch() || hasFluidOrEnergyToFetch();
    }

    /**
     * Whether a fluid or energy need found a container to draw from
     */
    private boolean hasFluidOrEnergyToFetch() {
        Brain<XoonglinEntity> brain = this.getBrain();
        return (brain.hasMemoryValue(CftMemoryModuleType.FLUID_CONTAINER.get())
                && !brain.hasMemoryValue(CftMemoryModuleType.FLUID_SUPPLY_COOLDOWN.get()))
                || (brain.hasMemoryValue(CftMemoryModuleType.ENERGY_CONTAINER.get())
                && !brain.hasMemoryValue(CftMemoryModuleType.ENERGY_SUPPLY_COOLDOWN.get()));
    }

    /** At home during the rest time of its schedule, or whenever it's time to go to bed. */
    private boolean shouldRest() {
        return this.home != null && (ScheduleUtils.is(this, Activity.REST) || shouldSleep());
    }

    private boolean shouldSleep() {
        return ScheduleUtils.isSleepTime(this)
                && (this.getBrain().hasMemoryValue(CftMemoryModuleType.MUST_SLEEP.get()) || this.isSleeping());
    }

    /** Whether it needs goods that a container it knows of holds, and isn't waiting to retry. */
    private boolean hasSuppliesToFetch() {
        Brain<XoonglinEntity> brain = this.getBrain();
        return !this.isSleeping()
                && brain.hasMemoryValue(CftMemoryModuleType.SUPPLIES_NEEDED.get())
                && brain.hasMemoryValue(CftMemoryModuleType.HOME_CONTAINER.get())
                && !brain.hasMemoryValue(CftMemoryModuleType.SUPPLY_COOLDOWN.get());
    }

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer
                && jobId != null && CftRegistry.JOBS.get(jobId) instanceof com.hyperbaton.cft.job.TraderJob
                && leaderId != null && !leaderId.equals(player.getUUID())) {
            com.hyperbaton.cft.network.OpenTradeScreenPacket.sendTo(serverPlayer, this);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, level);
        navigation.setCanOpenDoors(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return CftEntities.XOONGLIN.get().create(serverLevel);
    }

    /**
     * Fired by AgeableMob.setAge() exactly once whenever the baby/adult boundary is
     * crossed (natural growth or a forced ageUp), in either direction.
     */
    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!level().isClientSide) {
            assignEligibleJobIfNeeded();
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return CftSounds.XOONGLIN_AMBIENT.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return CftSounds.XOONGLIN_DEATH.get();
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
        return CftSounds.XOONGLIN_HURT.get();
    }

    @Override
    protected void updateWalkAnimation(float pPartialTick) {
        float f;
        if (this.getPose() == Pose.STANDING) {
            f = Math.min(pPartialTick * 6F, 1f);
        } else {
            f = 0f;
        }

        this.walkAnimation.update(f, 0.2f);
    }

    @Override
    protected Brain.Provider<XoonglinEntity> brainProvider() {
        return Brain.provider(XoonglinAi.MEMORY_TYPES, XoonglinAi.SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return XoonglinAi.makeBrain(brainProvider().makeBrain(dynamic));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Brain<XoonglinEntity> getBrain() {
        return (Brain<XoonglinEntity>) super.getBrain();
    }

    @Override
    public void die(DamageSource pDamageSource) {
        if (!this.level().isClientSide) {
            removeFromAllStructures();
            dropEquipmentAndInventory();
            cancelOwnRitual();
        }
        super.die(pDamageSource);
    }

    private void cancelOwnRitual() {
        RitualsData ritualsData = ((ServerLevel) level()).getDataStorage()
                .computeIfAbsent(RitualsData.factory(), "ritualsData");
        ritualsData.findByOfficiant(getUUID()).ifPresent(ritualsData::removeRitual);
    }

    private void dropEquipmentAndInventory() {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = getItemBySlot(slot);
            if (!equipped.isEmpty()) {
                spawnAtLocation(equipped);
                setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                spawnAtLocation(stack);
                inventory.removeItemNoUpdate(i);
            }
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);

        if (!this.level().isClientSide && CftConfig.KEEP_XOONGLINS_LOADED.get()) {
            ChunkPos chunkPos = new ChunkPos(this.blockPosition());
            ((ServerLevel) this.level()).getChunkSource().removeRegionTicket(
                    CftRegistry.XOONGLIN_CHUNK_TICKET,
                    chunkPos,
                    1,
                    this.getUUID()
            );
        }
    }

    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    private void checkSocialClass() {
        if (this.level().getPlayerByUUID(this.leaderId) == null) {
            return;
        }
        if (!downgradeSocialClass()) {
            upgradeSocialClass();
        }
    }

    private void upgradeSocialClass() {
        this.socialClass.getUpgrades().stream()
                .filter(this::appliesForUpgrade)
                .findAny()
                .ifPresent(socialClassUpdate -> changeSocialClass(socialClassUpdate.getNextClass(), true));
    }

    private boolean appliesForUpgrade(SocialClassUpdate socialClassUpdate) {
        return (!this.isBaby() || this.socialClass.canUpgradeAsBaby())
                && socialClassUpdate.getRequiredHappiness() < this.happiness
                && socialClassUpdate.getRequiredNeeds().stream().allMatch(needRequirement ->
                this.getNeeds().stream()
                        .filter(need -> need.getNeed().getId().equals(needRequirement.getNeed()))
                        .anyMatch(need -> need.getSatisfaction() > needRequirement.getSatisfactionThreshold()))
                && checkSocialStructureForUpgrade(
                getSocialStructureWithUpgrade(this.socialClass.getId(), socialClassUpdate.getNextClass()),
                socialClassUpdate);
    }

    private boolean checkSocialStructureForUpgrade(Map<SocialClass, Integer> socialStructure, SocialClassUpdate socialClassUpdate) {
        if (socialClassUpdate.getSocialStructureRequirements() != null) {
            return socialClassUpdate.getSocialStructureRequirements().stream().allMatch(socialStructureRequirement ->
                    SocialStructureHelper.computeScopedPercentage(socialStructure, socialStructureRequirement.getSocialClass(),
                            socialStructureRequirement.getScope()) > socialStructureRequirement.getPercentage());
        } else {
            return true;
        }
    }

    private boolean downgradeSocialClass() {
        Optional<SocialClassUpdate> optSocialClassToDowngradeTo = this.socialClass.getDowngrades().stream()
                .filter(this::appliesForDowngrade).findAny();
        optSocialClassToDowngradeTo.ifPresent(socialClassUpdate -> changeSocialClass(socialClassUpdate.getNextClass(), false));
        return optSocialClassToDowngradeTo.isPresent();
    }

    private boolean appliesForDowngrade(SocialClassUpdate socialClassUpdate) {
        return (!this.isBaby() || this.socialClass.canDowngradeAsBaby())
                && ((socialClassUpdate.getRequiredHappiness() > this.happiness
                && socialClassUpdate.getRequiredNeeds().stream().anyMatch(needRequirement ->
                this.getNeeds().stream()
                        .filter(need -> need.getNeed().getId().equals(needRequirement.getNeed()))
                        .anyMatch(need -> need.getSatisfaction() < needRequirement.getSatisfactionThreshold())))
                || checkSocialStructureForDowngrade(getSocialStructure(), socialClassUpdate));
    }

    private boolean checkSocialStructureForDowngrade(Map<SocialClass, Integer> socialStructure, SocialClassUpdate socialClassUpdate) {
        if (socialClassUpdate.getSocialStructureRequirements() != null) {
            return socialClassUpdate.getSocialStructureRequirements().stream().anyMatch(socialStructureRequirement ->
                    SocialStructureHelper.computeScopedPercentage(socialStructure, socialStructureRequirement.getSocialClass(),
                            socialStructureRequirement.getScope()) < socialStructureRequirement.getPercentage());
        } else {
            return false;
        }
    }

    private Map<SocialClass, Integer> getSocialStructure() {
        return SocialStructureHelper.computeSocialStructureForPlayer((ServerLevel) this.level(), (ServerPlayer) this.level().getPlayerByUUID(this.leaderId));
    }

    private Map<SocialClass, Integer> getSocialStructureWithUpgrade(String fromClass, String toClass) {
        return SocialStructureHelper.computeSocialStructureForPlayerWithUpgrade((ServerLevel) this.level(),
                (ServerPlayer) this.level().getPlayerByUUID(this.leaderId),
                fromClass,
                toClass);
    }

    private void changeSocialClass(String nextClass, boolean upgrade) {
        String previousClass = this.socialClass != null ? this.socialClass.getId() : null;
        this.socialClass = CftRegistry.SOCIAL_CLASSES.get(ResourceLocation.parse(nextClass));
        if (this.socialClass != null) {
            this.needs = NeedUtils.getNeedsForClass(this.socialClass);
            this.entityData.set(SOCIAL_CLASS_NAME, this.socialClass.getId());
            assignEligibleJobIfNeeded();
            resetMatingDelay();
            applyClassMaxHealth();
            notifyLeaderOfClassChange(previousClass, upgrade);
        }
        if (this.home != null) {
            this.home = null;
            this.getBrain().eraseMemory(CftMemoryModuleType.HOME_CONTAINER.get());
            this.getBrain().setMemory(CftMemoryModuleType.HOME_NEEDED.get(), true);
        }
        removeFromAllStructures();
    }

    public void applyClassMaxHealth() {
        if (this.socialClass == null) return;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.socialClass.getMaxHealth());
        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private void notifyLeaderOfClassChange(String previousClass, boolean upgrade) {
        if (leaderId != null && level().getPlayerByUUID(leaderId) instanceof ServerPlayer leader) {
            PacketDistributor.sendToPlayer(leader, new ClassChangeNotificationPacket(
                    getName().getString(), previousClass != null ? previousClass : "", socialClass.getId(), upgrade));
        }
    }

    /**
     * Publishes the worst visible need to the client, so it can be flagged above the
     * Xoonglin. Hidden and bonus needs are ignored: the former are technical and the
     * latter never cause unhappiness.
     */
    private void updateNeedAlert() {
        Optional<NeedSatisfier<? extends Need>> worst = getWorstNeed();
        byte level = worst.map(satisfier -> isCritical(satisfier) ? ALERT_CRITICAL : ALERT_UNSATISFIED).orElse(ALERT_NONE);
        String icon = worst.map(satisfier -> satisfier.getNeed().getIcons())
                .filter(icons -> !icons.isEmpty())
                .map(icons -> icons.get(0).toString())
                .orElse("");
        this.entityData.set(NEED_ALERT, level);
        this.entityData.set(NEED_ALERT_ICON, icon);
    }

    /**
     * The most pressing unsatisfied need: critical ones first, then the least satisfied.
     * Hidden and bonus needs are never reported. Server side only.
     */
    public Optional<NeedSatisfier<? extends Need>> getWorstNeed() {
        return getUnsatisfiedVisibleNeeds().stream()
                .min(Comparator.comparing((NeedSatisfier<? extends Need> satisfier) -> !isCritical(satisfier))
                        .thenComparingDouble(NeedSatisfier::getSatisfaction));
    }

    /** Unsatisfied needs that the player can see and that cause unhappiness. Server side only. */
    public List<NeedSatisfier<? extends Need>> getUnsatisfiedVisibleNeeds() {
        if (needs == null) return List.of();
        return needs.stream()
                .filter(satisfier -> !satisfier.getNeed().isHidden() && !satisfier.getNeed().isBonus() && !satisfier.isSatisfied())
                .toList();
    }

    public static boolean isCritical(NeedSatisfier<? extends Need> satisfier) {
        return satisfier.getNeed().getDamage() > 0.0
                && satisfier.getSatisfaction() < satisfier.getNeed().getDamageThreshold();
    }

    public byte getNeedAlert() {
        return this.entityData.get(NEED_ALERT);
    }

    public String getNeedAlertIcon() {
        return this.entityData.get(NEED_ALERT_ICON);
    }

    /** Client-safe leader lookup, backed by synced data. */
    public Optional<UUID> getSyncedLeaderId() {
        return this.entityData.get(LEADER_UUID);
    }

    private void removeFromAllStructures() {
        if (this.level().isClientSide) return;
        StructuresData structuresData = ((ServerLevel) this.level()).getDataStorage()
                .computeIfAbsent(StructuresData.factory(), "structuresData");
        for (Structure structure : structuresData.getStructures()) {
            structure.removeUser(this.getUUID());
        }
        structuresData.setDirty();
        assignedStructurePositions.clear();
    }

    public void removeFromJobStructures() {
        if (this.level().isClientSide) return;
        if (this.jobId == null) return;
        Job job = CftRegistry.JOBS.get(this.jobId);
        if (job == null) return;
        String structureType = job.getRequiredStructureType();
        if (structureType == null) return;
        BlockPos pos = assignedStructurePositions.remove(structureType);
        if (pos == null) return;
        StructuresData structuresData = ((ServerLevel) this.level()).getDataStorage()
                .computeIfAbsent(StructuresData.factory(), "structuresData");
        for (Structure structure : structuresData.getStructures()) {
            if (structure.getKeyBlockPos().equals(pos)) {
                structure.removeUser(this.getUUID());
                break;
            }
        }
        structuresData.setDirty();
    }

    public Map<String, BlockPos> getAssignedStructurePositions() {
        return assignedStructurePositions;
    }

    public void assignStructure(String structureTypeId, BlockPos keyBlockPos) {
        assignedStructurePositions.put(structureTypeId, keyBlockPos);
    }

    public void unassignStructure(String structureTypeId) {
        assignedStructurePositions.remove(structureTypeId);
    }

    public BlockPos getAssignedStructurePos(String structureTypeId) {
        return assignedStructurePositions.get(structureTypeId);
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = this.random.nextInt(80) + 160;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.1f)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5f)
                .add(Attributes.ATTACK_DAMAGE, 2f)
                .add(Attributes.FOLLOW_RANGE, 48D)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    /**
     * Decreases happiness for an unsatisfied need. Bonus needs never subtract happiness.
     */
    public void decreaseHappiness(Need need) {
        if (need.isBonus()) {
            return;
        }
        happiness = Math.max(
                happiness - (
                        need.getProvidedHappiness() *
                                BigDecimal.valueOf(DELAY_BETWEEN_NEEDS_CHECKS)
                                        .setScale(8, RoundingMode.HALF_UP)
                                        .divide(BigDecimal.valueOf(24000 * need.getFrequency()),
                                                RoundingMode.HALF_UP)
                                        .doubleValue()),
                0);
    }

    public void increaseHappiness(double providedHappiness, double frequency) {
        happiness = Math.min(
                happiness + (
                        providedHappiness *
                                BigDecimal.valueOf(DELAY_BETWEEN_NEEDS_CHECKS)
                                        .setScale(8, RoundingMode.HALF_UP)
                                        .divide(BigDecimal.valueOf(24000 * frequency),
                                                RoundingMode.HALF_UP)
                                        .doubleValue()),
                socialClass.getMaxHappiness());
    }

    public boolean allDamagingNeedsSatisfied() {
        return needs != null && needs.stream()
                .filter(needSatisfier -> needSatisfier.getNeed().getDamage() > 0.0)
                .allMatch(NeedSatisfier::isSatisfied);
    }

    public boolean canMate() {
        return !this.isBaby() &&
                matingDelay <= 0 &&
                this.socialClass != null &&
                this.happiness >= this.socialClass.getMatingHappinessThreshold() &&
                allDamagingNeedsSatisfied();
    }

    public void resetMatingDelay() {
        matingDelay = (socialClass != null && socialClass.getMatingDelay() >= 0)
                ? socialClass.getMatingDelay()
                : CftConfig.XOONGLIN_MATING_COOLDOWN.get();
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
        this.entityData.set(LEADER_UUID, Optional.ofNullable(leaderId));
    }

    public HouseStructure getHome() {
        return home;
    }

    public void setHome(HouseStructure home) {
        this.home = home;
    }

    protected ItemStack addToInventory(ItemStack pStack) {
        return this.inventory.addItem(pStack);
    }

    protected boolean canAddToInventory(ItemStack pStack) {
        return this.inventory.canAddItem(pStack);
    }

    public SocialClass getSocialClass() {
        return socialClass;
    }

    public void setSocialClass(SocialClass socialClass) {
        this.socialClass = socialClass;
    }

    public List<? extends NeedSatisfier<? extends Need>> getNeeds() {
        return needs;
    }

    public void setNeeds(List<NeedSatisfier<? extends Need>> needs) {
        this.needs = needs;
    }

    public double getHappiness() {
        return happiness;
    }

    public void setHappiness(double happiness) {
        this.happiness = happiness;
    }

    public void setJob(ResourceLocation jobId) { this.jobId = jobId; }

    public ResourceLocation getJob() { return jobId; }

    public void assignEligibleJobIfNeeded() {
        if (socialClass == null) return;

        Job currentJob = jobId != null ? CftRegistry.JOBS.get(jobId) : null;
        boolean currentJobValid = currentJob != null
                && socialClass.getJobs().contains(jobId)
                && (isBaby() ? currentJob.isAvailableToBabies() : currentJob.isAvailableToAdults());
        if (currentJobValid) return;

        ResourceLocation newJobId = socialClass.getRandomJob(getRandom(), isBaby());
        if (!Objects.equals(jobId, newJobId)) {
            setJob(newJobId);
            jobState.resetJobSpecific();
        }
    }

    public JobState getJobState() { return jobState; }

    public List<com.hyperbaton.cft.job.data.TradeOffer> getTradeOffers() {
        return tradeOffers;
    }

    public void setTradeOffer(int index, com.hyperbaton.cft.job.data.TradeOffer offer) {
        while (tradeOffers.size() <= index) {
            tradeOffers.add(com.hyperbaton.cft.job.data.TradeOffer.EMPTY);
        }
        tradeOffers.set(index, offer);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SOCIAL_CLASS_NAME, "xoonglin");
        builder.define(LEADER_UUID, Optional.empty());
        builder.define(NEED_ALERT, ALERT_NONE);
        builder.define(NEED_ALERT_ICON, "");
    }

    /**
     * Sized to fit the model in use: the custom Xoonglin model is about 1.6 blocks tall once
     * scaled, while the humanoid one is as tall as a zombie. Babies get half the size, as in
     * vanilla; the SCALE attribute is applied on top of this by {@link #getDimensions}.
     */
    @Override
    public @NotNull EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimensions = CftConfig.USE_HUMANOID_MODEL.get() ? HUMANOID_DIMENSIONS : XOONGLIN_DIMENSIONS;
        return dimensions.scale(this.getAgeScale());
    }

    @Override
    public void addAdditionalSaveData(final @NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(KEY_SOCIAL_CLASS, socialClass.getId());
        tag.putUUID(KEY_LEADER_ID, leaderId);
        tag.put(KEY_INVENTORY, inventory.createTag(this.registryAccess()));
        if (home != null) {
            tag.put(KEY_HOME, home.toTag());
        }
        tag.put(KEY_NEEDS, getNeedsTag(needs));
        tag.putDouble(KEY_HAPPINESS, happiness);
        if (jobId != null) {
            tag.putString(KEY_JOB_ID, jobId.toString());
            CompoundTag js = new CompoundTag();
            jobState.save(js);
            tag.put(KEY_JOB_STATE, js);
        }
        if (!assignedStructurePositions.isEmpty()) {
            CompoundTag structuresTag = new CompoundTag();
            for (Map.Entry<String, BlockPos> entry : assignedStructurePositions.entrySet()) {
                structuresTag.put(entry.getKey(), NbtUtils.writeBlockPos(entry.getValue()));
            }
            tag.put(KEY_ASSIGNED_STRUCTURES, structuresTag);
        }
        if (!tradeOffers.isEmpty()) {
            ListTag tradeOffersTag = new ListTag();
            for (com.hyperbaton.cft.job.data.TradeOffer offer : tradeOffers) {
                tradeOffersTag.add(offer.toTag(this.registryAccess()));
            }
            tag.put(KEY_TRADE_OFFERS, tradeOffersTag);
        }
    }

    private ListTag getNeedsTag(List<? extends NeedSatisfier<? extends Need>> needs) {

        ListTag needsTags = new ListTag();
        if (!this.needs.isEmpty()) {
            for (NeedSatisfier<? extends Need> need : needs) {
                needsTags.add(need.toTag());
            }
        }
        return needsTags;
    }

    @Override
    public void readAdditionalSaveData(final @NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(KEY_SOCIAL_CLASS, Tag.TAG_STRING)) {
            setSocialClass(CftRegistry.SOCIAL_CLASSES.get(ResourceLocation.parse(tag.getString(KEY_SOCIAL_CLASS))));
            this.entityData.set(SOCIAL_CLASS_NAME, this.socialClass.getId());
            applyClassMaxHealth();
        }
        if (tag.contains(KEY_LEADER_ID)) {
            setLeaderId(tag.getUUID(KEY_LEADER_ID));
        }
        if (tag.contains(KEY_INVENTORY)) {
            this.inventory.fromTag(tag.getList(KEY_INVENTORY, Tag.TAG_COMPOUND), this.registryAccess());
        }
        if (tag.contains(KEY_HOME)) {
            setHome(HouseStructure.fromTag(tag.getCompound(KEY_HOME)));
        }
        if (tag.contains(KEY_NEEDS)) {
            needs = new ArrayList<>();
            for (Tag needTag : tag.getList(KEY_NEEDS, Tag.TAG_COMPOUND)) {
                needs.add(NeedSatisfierMapper.mapNeedSatisfier((CompoundTag) needTag));
            }
        }
        if (tag.contains(KEY_HAPPINESS)) {
            setHappiness(tag.getDouble(KEY_HAPPINESS));
        }
        if (tag.contains(KEY_JOB_ID)) jobId = ResourceLocation.parse(tag.getString(KEY_JOB_ID));
        if (tag.contains(KEY_JOB_STATE)) jobState.load(tag.getCompound(KEY_JOB_STATE));
        if (tag.contains(KEY_ASSIGNED_STRUCTURES)) {
            assignedStructurePositions.clear();
            CompoundTag structuresTag = tag.getCompound(KEY_ASSIGNED_STRUCTURES);
            for (String key : structuresTag.getAllKeys()) {
                NbtUtils.readBlockPos(structuresTag, key).ifPresent(pos -> assignedStructurePositions.put(key, pos));
            }
        }
        if (tag.contains(KEY_TRADE_OFFERS)) {
            tradeOffers.clear();
            for (Tag offerTag : tag.getList(KEY_TRADE_OFFERS, Tag.TAG_COMPOUND)) {
                tradeOffers.add(com.hyperbaton.cft.job.data.TradeOffer.fromTag((CompoundTag) offerTag, this.registryAccess()));
            }
        }
    }

}
