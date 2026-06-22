package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.GathererJob;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.util.JobUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

public class GatherBlocksBehavior extends Behavior<XoonglinEntity> {

    private static final int REPATH_INTERVAL = 40;
    private static final double BLOCK_REACH = 2.5;
    private static final double HOME_REACH = 3.0;
    private static final int HARVEST_TICKS = 30;

    private enum State { SEARCHING, MOVING_TO_BLOCK, HARVESTING, RETURNING, DEPOSITING }

    private State state;
    private BlockPos targetBlock;
    private int repathTimer;
    private int harvestTimer;

    public GatherBlocksBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return getGathererJob(entity) != null && entity.getHome() != null
                && entity.getHome().getEntrance() != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        state = State.SEARCHING;
        targetBlock = null;
        repathTimer = 0;
        harvestTimer = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_GATHER.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        GathererJob job = getGathererJob(entity);
        if (job == null) return;

        switch (state) {
            case SEARCHING -> tickSearching(level, entity, job);
            case MOVING_TO_BLOCK -> tickMovingToBlock(entity);
            case HARVESTING -> tickHarvesting(level, entity, job);
            case RETURNING -> tickReturning(entity);
            case DEPOSITING -> tickDepositing(entity);
        }
    }

    private void tickSearching(ServerLevel level, XoonglinEntity entity, GathererJob job) {
        BlockPos home = entity.getHome().getEntrance();
        int radius = job.getGatherRadius();

        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                home.offset(-radius, -3, -radius),
                home.offset(radius, 3, radius))) {
            BlockState blockState = level.getBlockState(pos);
            if (job.matchesBlock(blockState)) {
                double dist = pos.distSqr(entity.blockPosition());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = pos.immutable();
                }
            }
        }

        if (closest != null) {
            targetBlock = closest;
            state = State.MOVING_TO_BLOCK;
            repathTimer = 0;
            navigateTo(entity, targetBlock);
        } else {
            navigateHome(entity);
            state = State.RETURNING;
            repathTimer = 0;
        }
    }

    private void tickMovingToBlock(XoonglinEntity entity) {
        if (targetBlock == null) {
            state = State.SEARCHING;
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(targetBlock)) < BLOCK_REACH) {
            entity.getNavigation().stop();
            state = State.HARVESTING;
            harvestTimer = 0;
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, targetBlock);
        }
    }

    private void tickHarvesting(ServerLevel level, XoonglinEntity entity, GathererJob job) {
        if (targetBlock == null) {
            state = State.SEARCHING;
            return;
        }

        BlockState blockState = level.getBlockState(targetBlock);
        if (!job.matchesBlock(blockState)) {
            state = State.SEARCHING;
            targetBlock = null;
            return;
        }

        if (++harvestTimer >= HARVEST_TICKS) {
            List<ItemStack> drops = Block.getDrops(blockState, level, targetBlock,
                    level.getBlockEntity(targetBlock), entity, entity.getMainHandItem());

            level.destroyBlock(targetBlock, false, entity);

            for (ItemStack drop : drops) {
                if (entity.getInventory().canAddItem(drop)) {
                    entity.getInventory().addItem(drop);
                }
            }

            targetBlock = null;

            if (hasItemsToDeposit(entity)) {
                state = State.RETURNING;
                repathTimer = 0;
                navigateHome(entity);
            } else {
                state = State.SEARCHING;
            }
        }
    }

    private void tickReturning(XoonglinEntity entity) {
        BlockPos entrance = entity.getHome() != null ? entity.getHome().getEntrance() : null;
        if (entrance == null) return;

        if (entity.position().distanceTo(Vec3.atCenterOf(entrance)) < HOME_REACH) {
            entity.getNavigation().stop();
            state = State.DEPOSITING;
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateHome(entity);
        }
    }

    private void tickDepositing(XoonglinEntity entity) {
        for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
            ItemStack stack = entity.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                ItemStack leftover = JobUtil.tryDepositAtHome(entity, stack.copy());
                if (leftover.isEmpty()) {
                    entity.getInventory().setItem(i, ItemStack.EMPTY);
                } else {
                    entity.getInventory().setItem(i, leftover);
                    JobUtil.dropAtHome(entity, leftover.copy());
                    entity.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }
        state = State.SEARCHING;
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
        targetBlock = null;
    }

    private boolean hasItemsToDeposit(XoonglinEntity entity) {
        for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
            if (!entity.getInventory().getItem(i).isEmpty()) return true;
        }
        return false;
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private void navigateHome(XoonglinEntity entity) {
        BlockPos entrance = entity.getHome() != null ? entity.getHome().getEntrance() : null;
        if (entrance != null) {
            entity.getNavigation().moveTo(entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5, 1.0);
        }
    }

    private GathererJob getGathererJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof GathererJob g ? g : null;
    }
}
