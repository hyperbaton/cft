package com.hyperbaton.cft.entity.custom;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.capability.need.ConsumeItemNeedCapability;
import com.hyperbaton.cft.capability.need.GoodsNeed;
import com.hyperbaton.cft.capability.need.NeedCapability;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.goal.GetSuppliesGoal;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.structure.home.XunguiHome;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class XunguiEntity extends AgeableMob implements InventoryCarrier {

    public static final int DELAY_BETWEEN_NEEDS_CHECKS = 20;
    public XunguiEntity(EntityType<? extends AgeableMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
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
        if(satisfyNeedsDelay > 0){
            satisfyNeedsDelay--;
        } else if(needs == null) {
            satisfyNeedsDelay = DELAY_BETWEEN_NEEDS_CHECKS;
        } else {
            for (NeedCapability currentNeed : needs) {
                if (currentNeed instanceof ConsumeItemNeedCapability) {
                    GoodsNeed need = ((ConsumeItemNeedCapability) currentNeed).getNeed();
                    if (currentNeed.getSatisfaction() < need.getSatisfactionThreshold()) {
                        if (inventory.hasAnyMatching(itemStack -> itemStack.is(need.getItem()) && itemStack.getCount() >= need.getQuantity())) {
                            // Consume item and satisfy the need
                            inventory.removeItemType(need.getItem(), need.getQuantity());
                            currentNeed.satisfy();
                            increaseHappiness(need.getProvidedHappiness(), need.getFrequency());
                        } else {
                            // Add goal for resupplying
                            this.goalSelector.addGoal(2, new GetSuppliesGoal(this, home.getContainerPos(), new ItemStack(need.getItem(), need.getQuantity())));
                            currentNeed.unsatisfy(need.getFrequency());
                            decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
                        }
                    } else {
                        currentNeed.unsatisfy(need.getFrequency());
                        increaseHappiness(need.getProvidedHappiness(), need.getFrequency());
                    }
                    currentNeed.setSatisfied(!(currentNeed.getSatisfaction() < need.getSatisfactionThreshold()));
                }
            }
            satisfyNeedsDelay = DELAY_BETWEEN_NEEDS_CHECKS;
        }

        // Remove all accomplished resupplying goals
        this.goalSelector.removeAllGoals(goal -> goal instanceof GetSuppliesGoal
        && ((GetSuppliesGoal) goal).getItemsToRetrieve().isEmpty());

        if (this.level().isClientSide()) {
            setupAnimationStates();
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
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 3f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
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

    private void decreaseHappiness(double providedHappiness, double frequency) {
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

    private void increaseHappiness(double providedHappiness, double frequency) {
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

    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
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
    public void addAdditionalSaveData(final @NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(KEY_SOCIAL_CLASS, socialClass.getId());
        tag.putUUID(KEY_LEADER_ID, leaderId);
        tag.put(KEY_INVENTORY, inventory.createTag());
        tag.put(KEY_HOME, home.toTag());
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
        if(tag.contains(KEY_SOCIAL_CLASS, Tag.TAG_STRING)) {
            setSocialClass(CftRegistry.SOCIAL_CLASSES.get(new ResourceLocation(tag.getString(KEY_SOCIAL_CLASS))));
        }
        if(tag.contains(KEY_LEADER_ID)) {
            setLeaderId(tag.getUUID(KEY_LEADER_ID));
        }
        if(tag.contains(KEY_INVENTORY)) {
            readInventoryFromTag(tag);
        }
        if(tag.contains(KEY_HOME)) {
            setHome(XunguiHome.fromTag(tag.getCompound(KEY_HOME)));
        }
        if(tag.contains(KEY_NEEDS)) {
            needs = new ArrayList<>();
            for(Tag needTag : tag.getList(KEY_NEEDS, Tag.TAG_COMPOUND)){
                needs.add(NeedCapability.fromTag((CompoundTag) needTag));
            }
        }
        if(tag.contains(KEY_HAPPINESS)) {
            setHappiness(tag.getDouble(KEY_HAPPINESS));
        }
    }
}