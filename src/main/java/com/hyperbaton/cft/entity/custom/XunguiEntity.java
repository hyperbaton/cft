package com.hyperbaton.cft.entity.custom;

import com.hyperbaton.cft.capability.need.ConsumeItemNeedCapability;
import com.hyperbaton.cft.capability.need.GoodsNeed;
import com.hyperbaton.cft.capability.need.INeedCapability;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.goal.GetSuppliesGoal;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.structure.home.XunguiHome;
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
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class XunguiEntity extends AgeableMob implements InventoryCarrier {
    public XunguiEntity(EntityType<? extends AgeableMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
    }

    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    private UUID leaderId;
    private final SimpleContainer inventory = new SimpleContainer(27);

    private XunguiHome home;

    private List<INeedCapability> needs;

    private SocialClass socialClass;

    private int satisfyNeedsDelay = 20;

    @Override
    public void tick() {
        super.tick();

        // Run satisfaction of needs every 20 ticks (1 second)
        if(satisfyNeedsDelay >= 0){
            satisfyNeedsDelay--;
        } else if(needs == null) {
            satisfyNeedsDelay = 20;
        } else {
            for (INeedCapability currentNeed : needs) {
                if (currentNeed instanceof ConsumeItemNeedCapability) {
                    GoodsNeed need = ((ConsumeItemNeedCapability) currentNeed).getNeed();
                    if (currentNeed.getSatisfaction() < need.getSatisfactionThreshold()) {
                        if (inventory.hasAnyMatching(itemStack -> itemStack.is(need.getItem()) && itemStack.getCount() >= need.getQuantity())) {
                            inventory.removeItemType(need.getItem(), need.getQuantity());
                            currentNeed.satisfy(need.getFrequency());
                        } else {
                            // TODO: Set goal to retrieve goods. Maybe through a memory in a brain?
                            currentNeed.unsatisfy(need.getFrequency());
                        }
                    } else {
                        currentNeed.unsatisfy(need.getFrequency());
                    }
                    currentNeed.setSatisfied(!(currentNeed.getSatisfaction() < need.getSatisfactionThreshold()));
                }
            }
            satisfyNeedsDelay = 20;
        }

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
        if(home != null){
            System.out.println("Adding getSuppliesGoal\n");
            this.goalSelector.addGoal(2, new GetSuppliesGoal(this, home.getContainerPos()));
        }
    }

    public void addHomeRelatedGoals(){
        if(home != null){
            System.out.println("Adding getSuppliesGoal\n");
            this.goalSelector.addGoal(2, new GetSuppliesGoal(this, home.getContainerPos()));
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

    public List<INeedCapability> getNeeds() {
        return needs;
    }

    public void setNeeds(List<INeedCapability> needs) {
        this.needs = needs;
    }
}
