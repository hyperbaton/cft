package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.FarmerJob;
import com.hyperbaton.cft.structure.OpenAirPlatformBlockGroup;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.JobUtil;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

public class FarmBehavior extends JobBehavior<FarmerJob> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REPATH_INTERVAL = 40;
    private static final double BLOCK_REACH = 2.5;
    private static final int ACTION_TICKS = 20;

    private enum State {
        SCANNING(WorkStep.of("checking_crops")),
        MOVING(WorkStep.of("walking_to_crops")),
        HARVESTING(WorkStep.of("harvesting")),
        PLANTING(WorkStep.of("planting")),
        MOVING_TO_CONTAINER(WorkStep.of("going_to_storage")),
        TAKING_SEEDS(WorkStep.of("getting_seeds")),
        DEPOSITING(WorkStep.STORING_ITEMS);

        /** Shown in the job tab while the behavior is in this state. */
        private final WorkStep step;

        State(WorkStep step) {
            this.step = step;
        }
    }

    private enum NextAction { HARVEST, PLANT }

    private State state;
    private NextAction nextAction;
    private BlockPos targetBlock;
    private BlockPos containerPos;
    private int repathTimer;
    private int actionTimer;

    public FarmBehavior() {
        super(CftMemoryModuleType.MUST_FARM.get(), FarmerJob.class, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return getJob(entity) != null && findAssignedStructure(level, entity) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        state = State.SCANNING;
        targetBlock = null;
        containerPos = null;
        repathTimer = 0;
        actionTimer = 0;
    }

    @Override
    protected WorkStep workStep() {
        return state != null ? state.step : null;
    }

    @Override
    protected void tickWork(ServerLevel level, XoonglinEntity entity, long gameTime) {
        FarmerJob job = getJob(entity);
        if (job == null) return;

        Structure structure = findAssignedStructure(level, entity);
        if (structure == null) return;

        switch (state) {
            case SCANNING -> tickScanning(level, entity, job, structure);
            case MOVING -> tickMoving(entity);
            case HARVESTING -> tickHarvesting(level, entity, job);
            case PLANTING -> tickPlanting(level, entity, job);
            case MOVING_TO_CONTAINER -> tickMovingToContainer(entity);
            case TAKING_SEEDS -> tickTakingSeeds(level, entity, job, structure);
            case DEPOSITING -> tickDepositing(level, entity, structure);
        }
    }

    private void tickScanning(ServerLevel level, XoonglinEntity entity, FarmerJob job, Structure structure) {
        List<BlockPos> surfaceBlocks = getSurfaceBlocks(structure);

        BlockPos ripeCrop = findRipeCrop(level, job, surfaceBlocks, entity);
        if (ripeCrop != null) {
            targetBlock = ripeCrop;
            nextAction = NextAction.HARVEST;
            state = State.MOVING;
            repathTimer = 0;
            navigateTo(entity, targetBlock);
            return;
        }

        BlockPos emptyFarmland = findPlantableSpot(level, job, surfaceBlocks, entity);
        if (emptyFarmland != null) {
            if (hasSeeds(entity, job.getSeed())) {
                targetBlock = emptyFarmland;
                nextAction = NextAction.PLANT;
                state = State.MOVING;
                repathTimer = 0;
                navigateTo(entity, targetBlock);
                return;
            }

            containerPos = findStructureContainer(level, structure);
            if (containerPos != null && containerHasSeeds(level, containerPos, job.getSeed())) {
                state = State.MOVING_TO_CONTAINER;
                repathTimer = 0;
                navigateTo(entity, containerPos);
                return;
            }
        }

        if (hasAnyItems(entity)) {
            containerPos = findStructureContainer(level, structure);
            if (containerPos != null) {
                state = State.MOVING_TO_CONTAINER;
                repathTimer = 0;
                navigateTo(entity, containerPos);
            } else {
                depositAtHome(entity, job);
            }
            return;
        }

        navigateTo(entity, structure.getKeyBlockPos());
    }

    private void tickMoving(XoonglinEntity entity) {
        if (targetBlock == null) {
            state = State.SCANNING;
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(targetBlock)) < BLOCK_REACH) {
            entity.getNavigation().stop();
            if (nextAction == NextAction.HARVEST) {
                state = State.HARVESTING;
            } else {
                state = State.PLANTING;
            }
            actionTimer = 0;
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, targetBlock);
        }
    }

    private void tickHarvesting(ServerLevel level, XoonglinEntity entity, FarmerJob job) {
        if (targetBlock == null) {
            state = State.SCANNING;
            return;
        }

        BlockState blockState = level.getBlockState(targetBlock);
        if (!job.isRipeCrop(blockState)) {
            state = State.SCANNING;
            targetBlock = null;
            return;
        }

        if (++actionTimer >= ACTION_TICKS) {
            List<ItemStack> drops = Block.getDrops(blockState, level, targetBlock,
                    level.getBlockEntity(targetBlock), entity, entity.getMainHandItem());

            level.destroyBlock(targetBlock, false, entity);

            for (ItemStack drop : drops) {
                if (entity.getInventory().canAddItem(drop)) {
                    entity.getInventory().addItem(drop);
                }
            }

            targetBlock = null;
            state = State.SCANNING;
        }
    }

    private void tickPlanting(ServerLevel level, XoonglinEntity entity, FarmerJob job) {
        if (targetBlock == null) {
            state = State.SCANNING;
            return;
        }

        if (!level.getBlockState(targetBlock).isAir()) {
            state = State.SCANNING;
            targetBlock = null;
            return;
        }

        if (++actionTimer >= ACTION_TICKS) {
            if (consumeSeed(entity, job.getSeed())) {
                BlockState cropState = job.getCropBlock().defaultBlockState();
                if (cropState.canSurvive(level, targetBlock)) {
                    level.setBlock(targetBlock, cropState, 3);
                }
            }
            targetBlock = null;
            state = State.SCANNING;
        }
    }

    private void tickMovingToContainer(XoonglinEntity entity) {
        if (containerPos == null) {
            state = State.SCANNING;
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(containerPos))
                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            entity.getNavigation().stop();
            if (hasAnyItems(entity)) {
                state = State.DEPOSITING;
            } else {
                state = State.TAKING_SEEDS;
            }
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, containerPos);
        }
    }

    private void tickTakingSeeds(ServerLevel level, XoonglinEntity entity, FarmerJob job, Structure structure) {
        if (containerPos == null || !(level.getBlockEntity(containerPos) instanceof Container container)) {
            state = State.SCANNING;
            return;
        }

        Ingredient seed = job.getSeed();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (seed.test(stack) && !stack.isEmpty()) {
                int takeAmount = Math.min(stack.getCount(), 16);
                ItemStack taken = container.removeItem(i, takeAmount);
                entity.getInventory().addItem(taken);
                container.setChanged();
                break;
            }
        }

        state = State.SCANNING;
    }

    private void tickDepositing(ServerLevel level, XoonglinEntity entity, Structure structure) {
        if (containerPos == null || !(level.getBlockEntity(containerPos) instanceof Container container)) {
            depositAtHome(entity, getJob(entity));
            state = State.SCANNING;
            return;
        }

        for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
            ItemStack stack = entity.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            for (int j = 0; j < container.getContainerSize(); j++) {
                ItemStack containerStack = container.getItem(j);
                if (containerStack.isEmpty()) {
                    container.setItem(j, stack.copy());
                    entity.getInventory().setItem(i, ItemStack.EMPTY);
                    break;
                } else if (ItemStack.isSameItemSameComponents(containerStack, stack)
                        && containerStack.getCount() < containerStack.getMaxStackSize()) {
                    int space = containerStack.getMaxStackSize() - containerStack.getCount();
                    int transfer = Math.min(space, stack.getCount());
                    containerStack.grow(transfer);
                    stack.shrink(transfer);
                    if (stack.isEmpty()) {
                        entity.getInventory().setItem(i, ItemStack.EMPTY);
                        break;
                    }
                }
            }
        }

        container.setChanged();
        state = State.SCANNING;
    }

    private void depositAtHome(XoonglinEntity entity, FarmerJob job) {
        for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
            ItemStack stack = entity.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            ItemStack leftover = JobUtil.tryDepositAtHome(entity, stack.copy());
            if (leftover.isEmpty()) {
                entity.getInventory().setItem(i, ItemStack.EMPTY);
            } else {
                entity.getInventory().setItem(i, leftover);
            }
        }
    }

    @Override
    protected void stopWork(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
        targetBlock = null;
        containerPos = null;
    }

    private BlockPos findRipeCrop(ServerLevel level, FarmerJob job, List<BlockPos> surfaceBlocks,
                                  XoonglinEntity entity) {
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (BlockPos surface : surfaceBlocks) {
            BlockPos above = surface.above();
            if (job.isRipeCrop(level.getBlockState(above))) {
                double dist = above.distSqr(entity.blockPosition());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = above;
                }
            }
        }
        return closest;
    }

    private BlockPos findPlantableSpot(ServerLevel level, FarmerJob job, List<BlockPos> surfaceBlocks,
                                       XoonglinEntity entity) {
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (BlockPos surface : surfaceBlocks) {
            BlockPos above = surface.above();
            if (level.getBlockState(above).isAir()) {
                BlockState cropState = job.getCropBlock().defaultBlockState();
                if (cropState.canSurvive(level, above)) {
                    double dist = above.distSqr(entity.blockPosition());
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = above;
                    }
                }
            }
        }
        return closest;
    }

    private BlockPos findStructureContainer(ServerLevel level, Structure structure) {
        List<BlockPos> borderBlocks = structure.getBlockPositions()
                .getOrDefault(OpenAirPlatformBlockGroup.BORDER.getKey(), Collections.emptyList());
        for (BlockPos pos : borderBlocks) {
            if (level.getBlockEntity(pos) instanceof Container) {
                return pos;
            }
        }
        return null;
    }

    private boolean containerHasSeeds(ServerLevel level, BlockPos pos, Ingredient seed) {
        if (!(level.getBlockEntity(pos) instanceof Container container)) return false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (seed.test(container.getItem(i)) && !container.getItem(i).isEmpty()) return true;
        }
        return false;
    }

    private boolean hasSeeds(XoonglinEntity entity, Ingredient seed) {
        for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
            ItemStack stack = entity.getInventory().getItem(i);
            if (seed.test(stack) && !stack.isEmpty()) return true;
        }
        return false;
    }

    private boolean consumeSeed(XoonglinEntity entity, Ingredient seed) {
        for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
            ItemStack stack = entity.getInventory().getItem(i);
            if (seed.test(stack) && !stack.isEmpty()) {
                entity.getInventory().removeItem(i, 1);
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyItems(XoonglinEntity entity) {
        for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
            if (!entity.getInventory().getItem(i).isEmpty()) return true;
        }
        return false;
    }

    private List<BlockPos> getSurfaceBlocks(Structure structure) {
        return structure.getBlockPositions()
                .getOrDefault(OpenAirPlatformBlockGroup.SURFACE.getKey(), Collections.emptyList());
    }

    private Structure findAssignedStructure(ServerLevel level, XoonglinEntity entity) {
        FarmerJob job = getJob(entity);
        if (job == null) return null;
        BlockPos structurePos = entity.getAssignedStructurePos(job.getRequiredStructureType());
        if (structurePos == null) return null;

        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        for (Structure structure : data.getStructures()) {
            if (structure.getKeyBlockPos().equals(structurePos)) {
                return structure;
            }
        }
        return null;
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

}
