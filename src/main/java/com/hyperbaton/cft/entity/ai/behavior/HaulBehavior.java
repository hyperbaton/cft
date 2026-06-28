package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.HaulerErrand;
import com.hyperbaton.cft.job.HaulerJob;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.structure.EnclosedBuildingBlockGroup;
import com.hyperbaton.cft.structure.OpenAirPlatformBlockGroup;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class HaulBehavior extends Behavior<XoonglinEntity> {

    private static final int REPATH_INTERVAL = 40;
    private static final double CONTAINER_REACH = 3.0;

    private enum State {
        PICKING_ERRAND, MOVING_TO_ORIGIN, TAKING_ITEMS,
        MOVING_TO_DESTINATION, DEPOSITING_ITEMS, RETURNING_ITEMS
    }

    private State state;
    private HaulerErrand currentErrand;
    private Structure originStructure;
    private Structure destinationStructure;
    private BlockPos originContainerPos;
    private BlockPos destinationContainerPos;
    private int repathTimer;

    public HaulBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return getHaulerJob(entity) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        state = State.PICKING_ERRAND;
        currentErrand = null;
        originStructure = null;
        destinationStructure = null;
        originContainerPos = null;
        destinationContainerPos = null;
        repathTimer = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_HAUL.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        HaulerJob job = getHaulerJob(entity);
        if (job == null) return;

        switch (state) {
            case PICKING_ERRAND -> tickPickingErrand(level, entity, job);
            case MOVING_TO_ORIGIN -> tickMovingToOrigin(entity);
            case TAKING_ITEMS -> tickTakingItems(level, entity);
            case MOVING_TO_DESTINATION -> tickMovingToDestination(entity);
            case DEPOSITING_ITEMS -> tickDepositingItems(level, entity);
            case RETURNING_ITEMS -> tickReturningItems(level, entity);
        }
    }

    private void tickPickingErrand(ServerLevel level, XoonglinEntity entity, HaulerJob job) {
        if (job.getErrands().isEmpty()) return;

        List<HaulerErrand> shuffled = new ArrayList<>(job.getErrands());
        Collections.shuffle(shuffled, new Random(entity.getRandom().nextLong()));

        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");

        for (HaulerErrand errand : shuffled) {
            Structure origin = findRandomStructure(data, entity, errand.originStructure(), job.getRadius());
            if (origin == null) continue;

            Structure destination = findRandomStructure(data, entity, errand.destinationStructure(), job.getRadius());
            if (destination == null) continue;

            BlockPos originContainer = findContainer(level, origin);
            if (originContainer == null) continue;

            BlockPos destContainer = findContainer(level, destination);
            if (destContainer == null) continue;

            currentErrand = errand;
            originStructure = origin;
            destinationStructure = destination;
            originContainerPos = originContainer;
            destinationContainerPos = destContainer;
            state = State.MOVING_TO_ORIGIN;
            repathTimer = 0;
            navigateTo(entity, originContainerPos);
            return;
        }
    }

    private void tickMovingToOrigin(XoonglinEntity entity) {
        if (originContainerPos == null) {
            state = State.PICKING_ERRAND;
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(originContainerPos)) < CONTAINER_REACH) {
            entity.getNavigation().stop();
            state = State.TAKING_ITEMS;
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, originContainerPos);
        }
    }

    private void tickTakingItems(ServerLevel level, XoonglinEntity entity) {
        if (originContainerPos == null || currentErrand == null
                || !(level.getBlockEntity(originContainerPos) instanceof Container container)) {
            state = State.PICKING_ERRAND;
            return;
        }

        boolean tookAnything = false;

        for (HaulerErrand.HaulerItem haulerItem : currentErrand.items()) {
            int remaining = haulerItem.quantity();
            Ingredient ingredient = haulerItem.ingredient();

            for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty() || !ingredient.test(stack)) continue;

                int takeAmount = Math.min(remaining, stack.getCount());
                ItemStack taken = container.removeItem(i, takeAmount);
                ItemStack leftover = entity.getInventory().addItem(taken);
                if (!leftover.isEmpty()) {
                    container.setItem(i, leftover);
                }
                int actuallyTaken = takeAmount - leftover.getCount();
                remaining -= actuallyTaken;
                if (actuallyTaken > 0) tookAnything = true;
            }
        }

        container.setChanged();

        if (!tookAnything) {
            state = State.PICKING_ERRAND;
            return;
        }

        state = State.MOVING_TO_DESTINATION;
        repathTimer = 0;
        navigateTo(entity, destinationContainerPos);
    }

    private void tickMovingToDestination(XoonglinEntity entity) {
        if (destinationContainerPos == null) {
            state = State.RETURNING_ITEMS;
            repathTimer = 0;
            navigateTo(entity, originContainerPos);
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(destinationContainerPos)) < CONTAINER_REACH) {
            entity.getNavigation().stop();
            state = State.DEPOSITING_ITEMS;
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, destinationContainerPos);
        }
    }

    private void tickDepositingItems(ServerLevel level, XoonglinEntity entity) {
        if (destinationContainerPos == null
                || !(level.getBlockEntity(destinationContainerPos) instanceof Container container)) {
            state = State.RETURNING_ITEMS;
            repathTimer = 0;
            navigateTo(entity, originContainerPos);
            return;
        }

        boolean hasLeftover = false;
        SimpleContainer inventory = entity.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !matchesErrand(stack)) continue;

            ItemStack remainder = insertIntoContainer(container, stack);
            inventory.setItem(i, remainder);
            if (!remainder.isEmpty()) hasLeftover = true;
        }
        container.setChanged();

        if (hasLeftover) {
            state = State.RETURNING_ITEMS;
            repathTimer = 0;
            navigateTo(entity, originContainerPos);
            return;
        }

        state = State.PICKING_ERRAND;
    }

    private void tickReturningItems(ServerLevel level, XoonglinEntity entity) {
        if (originContainerPos == null) {
            dropErrandItems(entity);
            state = State.PICKING_ERRAND;
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(originContainerPos)) < CONTAINER_REACH) {
            entity.getNavigation().stop();
            returnErrandItemsToContainer(level, entity, originContainerPos);
            state = State.PICKING_ERRAND;
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, originContainerPos);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();

        if (currentErrand != null) {
            returnErrandItemsToContainer(level, entity, originContainerPos);
        }

        currentErrand = null;
        originStructure = null;
        destinationStructure = null;
        originContainerPos = null;
        destinationContainerPos = null;
    }

    private Structure findRandomStructure(StructuresData data, XoonglinEntity entity, String structureTypeId, int radius) {
        BlockPos entityPos = entity.blockPosition();
        List<Structure> candidates = data.getStructures().stream()
                .filter(s -> s.getStructureTypeId().equals(structureTypeId))
                .filter(s -> s.getLeaderId().equals(entity.getLeaderId()))
                .filter(s -> s.getKeyBlockPos().distManhattan(entityPos) <= radius)
                .toList();

        if (candidates.isEmpty()) return null;
        return candidates.get(entity.getRandom().nextInt(candidates.size()));
    }

    private BlockPos findContainer(ServerLevel level, Structure structure) {
        for (String groupKey : List.of(
                EnclosedBuildingBlockGroup.INTERIOR.getKey(),
                OpenAirPlatformBlockGroup.BORDER.getKey())) {
            List<BlockPos> blocks = structure.getBlockPositions().getOrDefault(groupKey, Collections.emptyList());
            for (BlockPos pos : blocks) {
                if (level.getBlockEntity(pos) instanceof Container) {
                    return pos;
                }
            }
        }

        for (List<BlockPos> blocks : structure.getBlockPositions().values()) {
            for (BlockPos pos : blocks) {
                if (level.getBlockEntity(pos) instanceof Container) {
                    return pos;
                }
            }
        }

        return null;
    }

    private ItemStack insertIntoContainer(Container container, ItemStack stack) {
        ItemStack toInsert = stack.copy();
        for (int i = 0; i < container.getContainerSize() && !toInsert.isEmpty(); i++) {
            ItemStack existing = container.getItem(i);
            if (existing.isEmpty()) {
                container.setItem(i, toInsert.copy());
                toInsert = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(existing, toInsert)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int transfer = Math.min(space, toInsert.getCount());
                existing.grow(transfer);
                toInsert.shrink(transfer);
            }
        }
        return toInsert;
    }

    private boolean matchesErrand(ItemStack stack) {
        if (currentErrand == null) return false;
        for (HaulerErrand.HaulerItem haulerItem : currentErrand.items()) {
            if (haulerItem.ingredient().test(stack)) return true;
        }
        return false;
    }

    private void returnErrandItemsToContainer(ServerLevel level, XoonglinEntity entity, BlockPos containerPos) {
        SimpleContainer inventory = entity.getInventory();
        Container container = containerPos != null
                && level.getBlockEntity(containerPos) instanceof Container c ? c : null;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !matchesErrand(stack)) continue;

            if (container != null) {
                ItemStack remainder = insertIntoContainer(container, stack);
                inventory.setItem(i, remainder);
                if (!remainder.isEmpty()) {
                    dropItem(entity, remainder);
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            } else {
                dropItem(entity, stack);
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }

        if (container != null) container.setChanged();
    }

    private void dropErrandItems(XoonglinEntity entity) {
        SimpleContainer inventory = entity.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !matchesErrand(stack)) continue;
            dropItem(entity, stack);
            inventory.setItem(i, ItemStack.EMPTY);
        }
    }

    private void dropItem(XoonglinEntity entity, ItemStack stack) {
        net.minecraft.world.Containers.dropItemStack(
                entity.level(),
                entity.getX(), entity.getY(), entity.getZ(),
                stack);
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private HaulerJob getHaulerJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof HaulerJob h ? h : null;
    }
}
