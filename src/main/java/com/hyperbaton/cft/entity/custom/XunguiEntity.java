package com.hyperbaton.cft.entity.custom;

import com.google.common.collect.ImmutableList;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.capability.need.Need;
import com.hyperbaton.cft.capability.need.NeedCapability;
import com.hyperbaton.cft.capability.need.NeedCapabilityMapper;
import com.hyperbaton.cft.capability.need.NeedUtils;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.ai.XunguiAi;
import com.hyperbaton.cft.entity.ai.activity.CftActivities;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.socialclass.SocialClassUpdate;
import com.hyperbaton.cft.socialclass.SocialStructureHelper;
import com.hyperbaton.cft.structure.home.XunguiHome;
import com.hyperbaton.cft.world.HomesData;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class XunguiEntity extends AgeableMob implements InventoryCarrier {

    public static final int DELAY_BETWEEN_NEEDS_CHECKS = 20;
    private static final int MATING_COOLDOWN = 100; // In ticks (6000 - 5 minutes in Minecraft)
    public static final EntityDataAccessor<String> SOCIAL_CLASS_NAME = SynchedEntityData.defineId(XunguiEntity.class, EntityDataSerializers.STRING);

    public XunguiEntity(EntityType<? extends AgeableMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.setMaxUpStep(1.0F); // Allow stepping up one block
    }

    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    private UUID leaderId;
    private final SimpleContainer inventory = new SimpleContainer(27);

    private XunguiHome home;

    private List<NeedCapability> needs;

    private SocialClass socialClass;

    private double happiness = 0.0;

    private int satisfyNeedsDelay = DELAY_BETWEEN_NEEDS_CHECKS;
    private int matingDelay = MATING_COOLDOWN;

    // Tag keys
    private static final String KEY_LEADER_ID = "leaderId";
    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_HOME = "home";
    private static final String KEY_NEEDS = "needs";
    private static final String KEY_SOCIAL_CLASS = "socialClass";
    private static final String KEY_HAPPINESS = "happiness";


    @Override
    public void tick() {
        super.tick();

        // Run satisfaction of needs every 20 ticks (1 second)
        if (satisfyNeedsDelay > 0) {
            satisfyNeedsDelay--;
        } else if (needs == null) {
            satisfyNeedsDelay = DELAY_BETWEEN_NEEDS_CHECKS;
        } else {
            for (NeedCapability currentNeed : needs) {
                Need need = currentNeed.getNeed();
                if (currentNeed.getSatisfaction() < need.getSatisfactionThreshold()) {
                    currentNeed.satisfy(this);
                } else {
                    currentNeed.unsatisfy(need.getFrequency(), this);
                    increaseHappiness(need.getProvidedHappiness(), need.getFrequency());
                }
                currentNeed.setSatisfied(!(currentNeed.getSatisfaction() < need.getSatisfactionThreshold()));
            }
            checkSocialClass();
            satisfyNeedsDelay = DELAY_BETWEEN_NEEDS_CHECKS;
        }

        if (this.level().isClientSide()) {
            setupAnimationStates();
        }
    }

    @Override
    protected void customServerAiStep() {
        Brain<XunguiEntity> brain = this.getBrain();

        brain.tick((ServerLevel) level(), this);
        if (brain.getMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get()).isEmpty()
                || brain.hasMemoryValue(CftMemoryModuleType.SUPPLIES_NEEDED.get())) {
            brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.INVESTIGATE, Activity.IDLE));
        } else if (brain.getMemory(CftMemoryModuleType.CAN_MATE.get()).isPresent() &&
                brain.getMemory(CftMemoryModuleType.MATING_CANDIDATE.get()).isPresent()){
            brain.setActiveActivityToFirstValid(ImmutableList.of(CftActivities.MATE.get(), Activity.IDLE));
        } else {
            brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, level);
        navigation.setCanOpenDoors(true);  // Allows the entity to open doors
        navigation.setCanPassDoors(true); // Allows the pathfinder to consider doors as passable
        return navigation;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return CftEntities.XUNGUI.get().create(serverLevel);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HOGLIN_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.HUSK_HURT;
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
    protected Brain.Provider<XunguiEntity> brainProvider() {
        return Brain.provider(XunguiAi.MEMORY_TYPES, XunguiAi.SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return XunguiAi.makeBrain(brainProvider().makeBrain(dynamic));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Brain<XunguiEntity> getBrain() {
        return (Brain<XunguiEntity>) super.getBrain();
    }

    @Override
    public void die(DamageSource pDamageSource) {
        if (!this.level().isClientSide) {
            HomesData homesData = ((ServerLevel) this.level()).getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
            Optional<XunguiHome> mobHome = homesData.getHomes().stream().filter(
                    home -> home.getOwnerId() != null &&
                            home.getOwnerId().equals(this.uuid)
            ).findFirst();
            mobHome.ifPresent(xunguiHome -> xunguiHome.setOwnerId(null));
            homesData.setDirty();
        }
        super.die(pDamageSource);
    }

    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    private void checkSocialClass() {
        // TODO: Game crashed at startup because player is not yet loaded. Maybe there is a better way of loading or checking this?
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
                .ifPresent(socialClassUpdate -> changeSocialClass(socialClassUpdate.getNextClass()));
    }

    private boolean appliesForUpgrade(SocialClassUpdate socialClassUpdate) {
        return socialClassUpdate.getRequiredHappiness() < this.happiness
                && socialClassUpdate.getRequiredNeeds().stream().allMatch(needRequirement ->
                this.getNeeds().stream()
                        .filter(need -> need.getNeed().getId().equals(needRequirement.getNeed()))
                        .anyMatch(need -> need.getSatisfaction() > needRequirement.getSatisfactionThreshold())
                        && checkSocialStructureForUpgrade(
                        getNormalizedSocialStructureWithUpgrade(this.socialClass.getId(), socialClassUpdate.getNextClass()),
                        socialClassUpdate));
    }

    private boolean checkSocialStructureForUpgrade(Map<String, BigDecimal> socialStructure, SocialClassUpdate socialClassUpdate) {
        if (socialClassUpdate.getSocialStructureRequirements() != null) {
            return socialClassUpdate.getSocialStructureRequirements().stream().allMatch(socialStructureRequirement ->
                    socialStructure.get(socialStructureRequirement.getSocialClass()).doubleValue() > socialStructureRequirement.getPercentage());
        } else {
            return true;
        }
    }

    private boolean downgradeSocialClass() {
        Optional<SocialClassUpdate> optSocialClassToDowngradeTo = this.socialClass.getDowngrades().stream()
                .filter(this::appliesForDowngrade).findAny();
        optSocialClassToDowngradeTo.ifPresent(socialClassUpdate -> changeSocialClass(socialClassUpdate.getNextClass()));
        return optSocialClassToDowngradeTo.isPresent();
    }

    private boolean appliesForDowngrade(SocialClassUpdate socialClassUpdate) {
        return (socialClassUpdate.getRequiredHappiness() > this.happiness
                && socialClassUpdate.getRequiredNeeds().stream().anyMatch(needRequirement ->
                this.getNeeds().stream()
                        .filter(need -> need.getNeed().getId().equals(needRequirement.getNeed()))
                        .anyMatch(need -> need.getSatisfaction() < needRequirement.getSatisfactionThreshold())))
                || checkSocialStructureForDowngrade(getNormalizedSocialStructure(), socialClassUpdate);
    }

    private boolean checkSocialStructureForDowngrade(Map<String, BigDecimal> socialStructure, SocialClassUpdate socialClassUpdate) {
        if (socialClassUpdate.getSocialStructureRequirements() != null) {
            return socialClassUpdate.getSocialStructureRequirements().stream().anyMatch(socialStructureRequirement ->
                    socialStructure.get(socialStructureRequirement.getSocialClass()).doubleValue() < socialStructureRequirement.getPercentage());
        } else {
            return false;
        }
    }

    private Map<String, BigDecimal> getNormalizedSocialStructure() {
        return SocialStructureHelper.computeNormalizedSocialStructureForPlayer((ServerLevel) this.level(), (ServerPlayer) this.level().getPlayerByUUID(this.leaderId))
                .entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getId(), Map.Entry::getValue));
    }

    private Map<String, BigDecimal> getNormalizedSocialStructureWithUpgrade(String fromClass, String toClass) {
        return SocialStructureHelper.computeNormalizedSocialStructureForPlayerWithUpgrade((ServerLevel) this.level(),
                        (ServerPlayer) this.level().getPlayerByUUID(this.leaderId),
                        fromClass,
                        toClass)
                .entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getId(), Map.Entry::getValue));
    }

    private void changeSocialClass(String nextClass) {
        this.socialClass = CftRegistry.SOCIAL_CLASSES.get(new ResourceLocation(nextClass));
        if (this.socialClass != null) {
            this.needs = NeedUtils.getNeedsForClass(this.socialClass);
            this.entityData.set(SOCIAL_CLASS_NAME, this.socialClass.getId());
        }
        if (this.home != null) {
            HomesData homesData = ((ServerLevel) this.level()).getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
            homesData.getHomes().stream().filter(home -> home.getOwnerId() != null
                    && home.getOwnerId().equals(this.uuid)).findFirst().ifPresent(home -> home.setOwnerId(null));
            homesData.setDirty();
            this.home = null;
            this.getBrain().eraseMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get());
            this.getBrain().setMemory(CftMemoryModuleType.HOME_NEEDED.get(), true);
        }
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
                .add(Attributes.FOLLOW_RANGE, 24D);
    }

    public void decreaseHappiness(double providedHappiness, double frequency) {
        happiness = Math.max(
                happiness - (
                        providedHappiness *
                                BigDecimal.valueOf(DELAY_BETWEEN_NEEDS_CHECKS)
                                        .setScale(8, RoundingMode.HALF_UP)
                                        .divide(BigDecimal.valueOf(24000 * frequency),
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

    /**
     * A xungui can mate if it didn't mate recently, its happiness is over the mating threshold and
     * all its needs are satisfied
     * @return Whether or not the xungui can mate
     */
    public boolean canMate() {
        matingDelay --;
        return !this.isBaby() &&
                matingDelay <= 0 &&
                this.socialClass != null &&
                this.happiness >= this.socialClass.getMatingHappinessThreshold() &&
                this.needs.stream().allMatch(NeedCapability::isSatisfied);
    }

    public void resetMatingDelay(){
        matingDelay = MATING_COOLDOWN;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
    }

    public XunguiHome getHome() {
        return home;
    }

    public void setHome(XunguiHome home) {
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

    public List<NeedCapability> getNeeds() {
        return needs;
    }

    public void setNeeds(List<NeedCapability> needs) {
        this.needs = needs;
    }

    public double getHappiness() {
        return happiness;
    }

    public void setHappiness(double happiness) {
        this.happiness = happiness;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SOCIAL_CLASS_NAME, "xungui");
    }

    @Override
    public void addAdditionalSaveData(final @NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(KEY_SOCIAL_CLASS, socialClass.getId());
        tag.putUUID(KEY_LEADER_ID, leaderId);
        tag.put(KEY_INVENTORY, inventory.createTag());
        if (home != null) {
            tag.put(KEY_HOME, home.toTag());
        }
        tag.put(KEY_NEEDS, getNeedsTag(needs));
        tag.putDouble(KEY_HAPPINESS, happiness);
    }

    private ListTag getNeedsTag(List<NeedCapability> needs) {

        ListTag needsTags = new ListTag();
        if (!this.needs.isEmpty()) {
            for (NeedCapability need : needs) {
                needsTags.add(need.toTag());
            }
        }
        return needsTags;
    }

    @Override
    public void readAdditionalSaveData(final @NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(KEY_SOCIAL_CLASS, Tag.TAG_STRING)) {
            setSocialClass(CftRegistry.SOCIAL_CLASSES.get(new ResourceLocation(tag.getString(KEY_SOCIAL_CLASS))));
            this.entityData.set(SOCIAL_CLASS_NAME, this.socialClass.getId());
        }
        if (tag.contains(KEY_LEADER_ID)) {
            setLeaderId(tag.getUUID(KEY_LEADER_ID));
        }
        if (tag.contains(KEY_INVENTORY)) {
            readInventoryFromTag(tag);
        }
        if (tag.contains(KEY_HOME)) {
            setHome(XunguiHome.fromTag(tag.getCompound(KEY_HOME)));
        }
        if (tag.contains(KEY_NEEDS)) {
            needs = new ArrayList<>();
            for (Tag needTag : tag.getList(KEY_NEEDS, Tag.TAG_COMPOUND)) {
                needs.add(NeedCapabilityMapper.mapNeedCapability((CompoundTag) needTag));
            }
        }
        if (tag.contains(KEY_HAPPINESS)) {
            setHappiness(tag.getDouble(KEY_HAPPINESS));
        }
    }
}