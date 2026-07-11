package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.ItemQuantity;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.job.RancherJob;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Drives a rancher: keep a stock of feed and empty buckets from the pasture's container,
 * then repeatedly find and perform whichever of shearing, milking or feeding-to-breed is
 * closest and ready, on its own cooldown. Wool and milk are deposited back in the
 * container; feed is consumed from it.
 */
public class RanchBehavior extends Behavior<XoonglinEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REPATH_INTERVAL = 40;
    private static final double ACTION_REACH = 2.5;
    private static final int MAX_NAV_FAILURES = 5;
    // Ticks to idle before scanning again when there is nothing to do
    private static final int IDLE_WAIT = 40;
    // Ticks to wait before re-checking the base when it lacks supplies
    private static final int RESTOCK_COOLDOWN = 600;
    private static final int VERTICAL_PADDING = 2;
    private static final String TAG_LAST_MILKED = "cft_last_milked";

    private enum ActionType {
        SHEAR, MILK, FEED
    }

    private record PendingAction(ActionType type, UUID primaryId, UUID secondaryId) {}

    private enum State {
        FETCHING, SEEKING, WORKING, WAITING
    }

    private State state;
    private BlockPos basePos;
    private BlockPos fetchPos;
    private PendingAction action;
    private int repathTimer;
    private int waitTicks;
    private int navFailures;
    private int shearCooldown;
    private int milkCooldown;
    private int feedCooldown;

    public RanchBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        RancherJob job = getRancherJob(entity);
        return job != null && entity.getAssignedStructurePos(job.getRequiredStructureType()) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        RancherJob job = getRancherJob(entity);
        basePos = entity.getAssignedStructurePos(job.getRequiredStructureType());
        state = State.SEEKING;
        fetchPos = null;
        action = null;
        repathTimer = 0;
        waitTicks = 0;
        navFailures = 0;
        shearCooldown = 0;
        milkCooldown = 0;
        feedCooldown = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_RANCH.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        RancherJob job = getRancherJob(entity);
        if (job == null || basePos == null) return;

        if (shearCooldown > 0) shearCooldown--;
        if (milkCooldown > 0) milkCooldown--;
        if (feedCooldown > 0) feedCooldown--;

        if ((state == State.SEEKING || state == State.WAITING) && needsRestock(entity, job)
                && baseHasSupplies(level, entity, job)) {
            state = State.FETCHING;
            fetchPos = null;
            repathTimer = 0;
            navFailures = 0;
        }

        switch (state) {
            case FETCHING -> tickFetching(level, entity, job);
            case SEEKING -> tickSeeking(level, entity, job);
            case WORKING -> tickWorking(level, entity, job);
            case WAITING -> tickWaiting();
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
        action = null;
    }

    // --- Fetching ---

    private void tickFetching(ServerLevel level, XoonglinEntity entity, RancherJob job) {
        List<Container> containers = baseContainers(level, job);
        if (containers.isEmpty()) {
            LOGGER.warn("[Ranch] {} has no containers at base {}, waiting",
                    entity.getName().getString(), basePos);
            state = State.WAITING;
            waitTicks = RESTOCK_COOLDOWN;
            return;
        }

        if (fetchPos == null) {
            fetchPos = baseContainerPositions(level, job).stream().findFirst().orElse(basePos);
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(fetchPos))
                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            entity.getNavigation().stop();
            takeSupplies(entity, job, containers);
            containers.forEach(Container::setChanged);
            state = State.SEEKING;
            return;
        }

        if (entity.getNavigation().isDone()) {
            if (++navFailures >= MAX_NAV_FAILURES) {
                navFailures = 0;
                takeSupplies(entity, job, containers);
                containers.forEach(Container::setChanged);
                state = State.SEEKING;
                return;
            }
            repathTimer = 0;
            navigateTo(entity, fetchPos);
        } else if (++repathTimer >= REPATH_INTERVAL) {
            repathTimer = 0;
            navigateTo(entity, fetchPos);
        }
    }

    private boolean needsRestock(XoonglinEntity entity, RancherJob job) {
        boolean needsFeed = !job.getFeed().isEmpty() && !hasFeedPair(entity, job);
        boolean needsBucket = job.isMilkEnabled() && !hasBucket(entity);
        boolean needsShears = job.isShearEnabled() && !hasShears(entity);
        return needsFeed || needsBucket || needsShears;
    }

    private boolean baseHasSupplies(ServerLevel level, XoonglinEntity entity, RancherJob job) {
        List<Container> containers = baseContainers(level, job);
        if (containers.isEmpty()) return false;
        for (ItemQuantity item : job.getFeed()) {
            if (countInContainers(containers, item.ingredient()::test) >= item.quantity() * 2) return true;
        }
        if (job.isMilkEnabled() && countInContainers(containers, stack -> stack.is(Items.BUCKET)) > 0) return true;
        if (job.isShearEnabled() && countInContainers(containers, stack -> stack.is(Items.SHEARS)) > 0) return true;
        return false;
    }

    private void takeSupplies(XoonglinEntity entity, RancherJob job, List<Container> containers) {
        for (ItemQuantity item : job.getFeed()) {
            int wanted = item.quantity() * job.getDosesPerFetch();
            takeFromContainers(entity, containers, item.ingredient()::test, wanted);
        }
        if (job.isMilkEnabled()) {
            takeFromContainers(entity, containers, stack -> stack.is(Items.BUCKET), job.getBucketsPerFetch());
        }
        if (job.isShearEnabled() && !hasShears(entity)) {
            takeFromContainers(entity, containers, stack -> stack.is(Items.SHEARS), 1);
        }
    }

    private void takeFromContainers(XoonglinEntity entity, List<Container> containers,
                                     Predicate<ItemStack> matcher, int wanted) {
        for (Container container : containers) {
            for (int i = 0; i < container.getContainerSize() && wanted > 0; i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty() || !matcher.test(stack)) continue;
                int take = Math.min(wanted, stack.getCount());
                ItemStack taken = stack.copyWithCount(take);
                ItemStack leftover = entity.getInventory().addItem(taken);
                int actuallyTaken = take - leftover.getCount();
                stack.shrink(actuallyTaken);
                wanted -= actuallyTaken;
                if (!leftover.isEmpty()) {
                    wanted = 0; // inventory full
                }
            }
        }
    }

    // --- Seeking ---

    private void tickSeeking(ServerLevel level, XoonglinEntity entity, RancherJob job) {
        Structure structure = structureOrNull(level, job);
        if (structure == null) {
            state = State.WAITING;
            waitTicks = IDLE_WAIT;
            return;
        }

        AABB area = boundingBox(structure);
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, area, Animal::isAlive);

        PendingAction best = null;
        double bestDistSqr = Double.MAX_VALUE;

        if (job.isShearEnabled() && shearCooldown <= 0 && hasShears(entity)) {
            for (Animal animal : animals) {
                if (animal instanceof Sheep sheep && sheep.readyForShearing()) {
                    double d = entity.position().distanceToSqr(sheep.position());
                    if (d < bestDistSqr) {
                        bestDistSqr = d;
                        best = new PendingAction(ActionType.SHEAR, sheep.getUUID(), null);
                    }
                }
            }
        }

        if (job.isMilkEnabled() && milkCooldown <= 0 && hasBucket(entity)) {
            for (Animal animal : animals) {
                if (animal instanceof Cow cow && !cow.isBaby() && isMilkable(cow, level, job)) {
                    double d = entity.position().distanceToSqr(cow.position());
                    if (d < bestDistSqr) {
                        bestDistSqr = d;
                        best = new PendingAction(ActionType.MILK, cow.getUUID(), null);
                    }
                }
            }
        }

        if (!job.getFeed().isEmpty() && feedCooldown <= 0) {
            Map<EntityType<?>, List<Animal>> groups = new HashMap<>();
            for (Animal animal : animals) {
                if (animal.isBaby() || !animal.canFallInLove()) continue;
                groups.computeIfAbsent(animal.getType(), t -> new ArrayList<>()).add(animal);
            }
            for (List<Animal> group : groups.values()) {
                if (group.size() < 2) continue;
                Animal sample = group.get(0);
                ItemQuantity matching = findMatchingFeed(job, sample);
                if (matching == null || !hasQuantity(entity, matching.ingredient()::test, matching.quantity() * 2)) {
                    continue;
                }
                Animal a = group.get(0);
                Animal b = group.get(1);
                double d = entity.position().distanceToSqr(a.position());
                if (d < bestDistSqr) {
                    bestDistSqr = d;
                    best = new PendingAction(ActionType.FEED, a.getUUID(), b.getUUID());
                }
            }
        }

        if (best == null) {
            state = State.WAITING;
            waitTicks = IDLE_WAIT;
            return;
        }

        action = best;
        state = State.WORKING;
        repathTimer = 0;
        navFailures = 0;
    }

    private ItemQuantity findMatchingFeed(RancherJob job, Animal sample) {
        for (ItemQuantity item : job.getFeed()) {
            ItemStack[] matches = item.ingredient().getItems();
            if (matches.length > 0 && sample.isFood(matches[0])) {
                return item;
            }
        }
        return null;
    }

    // --- Working ---

    private void tickWorking(ServerLevel level, XoonglinEntity entity, RancherJob job) {
        if (action == null) {
            state = State.SEEKING;
            return;
        }

        Entity primary = level.getEntity(action.primaryId());
        if (!(primary instanceof Animal primaryAnimal) || !primaryAnimal.isAlive()
                || primaryAnimal.blockPosition().distManhattan(basePos) > 64) {
            action = null;
            state = State.SEEKING;
            return;
        }

        Animal secondaryAnimal = null;
        if (action.type() == ActionType.FEED) {
            Entity secondary = level.getEntity(action.secondaryId());
            if (!(secondary instanceof Animal a) || !a.isAlive() || a.isBaby() || !a.canFallInLove()
                    || primaryAnimal.isBaby() || !primaryAnimal.canFallInLove()) {
                action = null;
                state = State.SEEKING;
                return;
            }
            secondaryAnimal = a;
        }

        // Re-verify the target is still eligible: it may have been sheared/milked by
        // someone else, or run out of milk cooldown, while the xoonglin was traveling
        if (action.type() == ActionType.SHEAR && !(primaryAnimal instanceof Sheep sheep && sheep.readyForShearing())) {
            action = null;
            state = State.SEEKING;
            return;
        }
        if (action.type() == ActionType.MILK
                && !(primaryAnimal instanceof Cow cow && isMilkable(cow, level, job))) {
            action = null;
            state = State.SEEKING;
            return;
        }

        double distance = entity.position().distanceTo(primaryAnimal.position());
        if (distance > ACTION_REACH) {
            if (entity.getNavigation().isDone()) {
                if (++navFailures >= MAX_NAV_FAILURES) {
                    navFailures = 0;
                    action = null;
                    state = State.SEEKING;
                    return;
                }
                repathTimer = 0;
                navigateTo(entity, primaryAnimal.blockPosition());
            } else if (++repathTimer >= REPATH_INTERVAL) {
                repathTimer = 0;
                navigateTo(entity, primaryAnimal.blockPosition());
            }
            return;
        }

        entity.getNavigation().stop();
        // BlockPosTracker (not EntityTracker): EntityTracker.isVisibleBy reads the
        // visible_mobs memory, which xoonglin brains do not register, and would crash
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,
                new BlockPosTracker(primaryAnimal.getEyePosition()));

        switch (action.type()) {
            case SHEAR -> performShear(level, entity, job, (Sheep) primaryAnimal);
            case MILK -> performMilk(level, entity, job, (Cow) primaryAnimal);
            case FEED -> performFeed(level, entity, job, primaryAnimal, secondaryAnimal);
        }

        action = null;
        state = State.SEEKING;
    }

    private void performShear(ServerLevel level, XoonglinEntity entity, RancherJob job, Sheep sheep) {
        if (!hasShears(entity)) {
            shearCooldown = job.getActionCooldown();
            return;
        }
        List<Container> containers = baseContainers(level, job);
        Item woolItem = woolItem(sheep.getColor());
        ItemStack wool = new ItemStack(woolItem, 1);
        ItemStack leftover = ContainerUtil.insertIntoContainers(containers, wool);
        sheep.setSheared(true);
        useShears(entity);
        entity.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                sheep.getX(), sheep.getEyeY(), sheep.getZ(), 6, 0.4, 0.4, 0.4, 0.1);
        if (!leftover.isEmpty()) {
            Containers.dropItemStack(level, basePos.getX() + 0.5,
                    basePos.getY() + 1.0, basePos.getZ() + 0.5, leftover);
        }
        shearCooldown = job.getActionCooldown();
    }

    private void performMilk(ServerLevel level, XoonglinEntity entity, RancherJob job, Cow cow) {
        if (!hasQuantity(entity, stack -> stack.is(Items.BUCKET), 1)) {
            milkCooldown = job.getActionCooldown();
            return;
        }
        removeFromInventory(entity, stack -> stack.is(Items.BUCKET), 1);
        List<Container> containers = baseContainers(level, job);
        ItemStack milkBucket = new ItemStack(Items.MILK_BUCKET, 1);
        ItemStack leftover = ContainerUtil.insertIntoContainers(containers, milkBucket);
        markMilked(cow, level);
        entity.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                cow.getX(), cow.getEyeY(), cow.getZ(), 4, 0.4, 0.4, 0.4, 0.1);
        if (!leftover.isEmpty()) {
            Containers.dropItemStack(level, basePos.getX() + 0.5,
                    basePos.getY() + 1.0, basePos.getZ() + 0.5, leftover);
        }
        milkCooldown = job.getActionCooldown();
    }

    private void performFeed(ServerLevel level, XoonglinEntity entity, RancherJob job, Animal a, Animal b) {
        ItemQuantity matching = findMatchingFeed(job, a);
        if (matching == null || !hasQuantity(entity, matching.ingredient()::test, matching.quantity() * 2)) {
            feedCooldown = job.getActionCooldown();
            return;
        }
        removeFromInventory(entity, matching.ingredient()::test, matching.quantity() * 2);
        a.setInLove(null);
        b.setInLove(null);
        entity.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.HEART, a.getX(), a.getEyeY(), a.getZ(), 3, 0.3, 0.3, 0.3, 0.1);
        level.sendParticles(ParticleTypes.HEART, b.getX(), b.getEyeY(), b.getZ(), 3, 0.3, 0.3, 0.3, 0.1);
        feedCooldown = job.getActionCooldown();
    }

    private void tickWaiting() {
        if (--waitTicks <= 0) {
            state = State.SEEKING;
        }
    }

    // --- Helpers ---

    private static final Map<DyeColor, Item> WOOL_ITEMS = new HashMap<>();

    private Item woolItem(DyeColor color) {
        return WOOL_ITEMS.computeIfAbsent(color, c ->
                BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(c.getName() + "_wool")));
    }

    private boolean hasBucket(XoonglinEntity entity) {
        return hasQuantity(entity, stack -> stack.is(Items.BUCKET), 1);
    }

    private boolean hasShears(XoonglinEntity entity) {
        return hasQuantity(entity, stack -> stack.is(Items.SHEARS), 1);
    }

    /** Damages the carried shears by one use, discarding them if they break. */
    private void useShears(XoonglinEntity entity) {
        SimpleContainer inventory = entity.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(Items.SHEARS)) {
                stack.setDamageValue(stack.getDamageValue() + 1);
                if (stack.getDamageValue() >= stack.getMaxDamage()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    /**
     * Vanilla cows have no cooldown on milking at all, so without one a rancher would
     * just spam the same cow forever. milk_regen_ticks adds a per-cow cooldown, tracked
     * in the cow's own persistent data so it survives chunk unload/reload.
     */
    private boolean isMilkable(Cow cow, ServerLevel level, RancherJob job) {
        if (cow.isBaby()) return false;
        long lastMilked = cow.getPersistentData().getLong(TAG_LAST_MILKED);
        return level.getGameTime() - lastMilked >= job.getMilkRegenTicks();
    }

    private void markMilked(Cow cow, ServerLevel level) {
        CompoundTag data = cow.getPersistentData();
        data.putLong(TAG_LAST_MILKED, level.getGameTime());
    }

    private boolean hasFeedPair(XoonglinEntity entity, RancherJob job) {
        for (ItemQuantity item : job.getFeed()) {
            if (hasQuantity(entity, item.ingredient()::test, item.quantity() * 2)) return true;
        }
        return false;
    }

    private boolean hasQuantity(XoonglinEntity entity, Predicate<ItemStack> matcher, int quantity) {
        SimpleContainer inventory = entity.getInventory();
        int found = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && matcher.test(stack)) {
                found += stack.getCount();
                if (found >= quantity) return true;
            }
        }
        return false;
    }

    private void removeFromInventory(XoonglinEntity entity, Predicate<ItemStack> matcher, int quantity) {
        SimpleContainer inventory = entity.getInventory();
        int remaining = quantity;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && matcher.test(stack)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    private int countInContainers(List<Container> containers, Predicate<ItemStack> matcher) {
        int found = 0;
        for (Container container : containers) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && matcher.test(stack)) {
                    found += stack.getCount();
                }
            }
        }
        return found;
    }

    private Structure structureOrNull(ServerLevel level, RancherJob job) {
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        return data.getStructures().stream()
                .filter(s -> s.getKeyBlockPos().equals(basePos))
                .filter(s -> s.getStructureTypeId().equals(job.getRequiredStructureType()))
                .findFirst().orElse(null);
    }

    private AABB boundingBox(Structure structure) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (List<BlockPos> group : structure.getBlockPositions().values()) {
            for (BlockPos pos : group) {
                minX = Math.min(minX, pos.getX());
                maxX = Math.max(maxX, pos.getX());
                minY = Math.min(minY, pos.getY());
                maxY = Math.max(maxY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxZ = Math.max(maxZ, pos.getZ());
            }
        }
        return new AABB(minX, minY, minZ, maxX + 1, maxY + VERTICAL_PADDING, maxZ + 1);
    }

    private List<BlockPos> baseContainerPositions(ServerLevel level, RancherJob job) {
        Structure structure = structureOrNull(level, job);
        if (structure == null) return List.of();
        return ContainerUtil.findContainerPositions(level, structure);
    }

    private List<Container> baseContainers(ServerLevel level, RancherJob job) {
        Structure structure = structureOrNull(level, job);
        if (structure == null) return List.of();
        return ContainerUtil.findContainers(level, structure);
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private RancherJob getRancherJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof RancherJob r ? r : null;
    }
}
