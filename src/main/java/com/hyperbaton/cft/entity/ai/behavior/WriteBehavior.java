package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.job.WriterJob;
import com.hyperbaton.cft.util.ContainerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * Walks a writer to its base (assigned structure if configured, otherwise home). If an
 * input is configured and not already carried, it's fetched into the writer's inventory
 * first — production itself happens inside WriterJob.tick() once the writer is at base
 * and holding the input; this behavior's job is just to get it there and stocked.
 */
public class WriteBehavior extends Behavior<XoonglinEntity> {

    private static final int REPATH_INTERVAL = 40;
    // Ticks to wait before re-checking the container when the input isn't available
    private static final int NO_INPUT_RETRY_COOLDOWN = 600;

    private enum State {
        FETCHING, TRAVELING
    }

    private State state;
    private int repathTimer;
    private int waitTicks;

    public WriteBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        WriterJob job = getWriterJob(entity);
        if (job == null) return false;
        return needsFetch(entity, job) || (basePos(entity) != null && !isAtBase(entity));
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        WriterJob job = getWriterJob(entity);
        repathTimer = 0;
        waitTicks = 0;
        state = needsFetch(entity, job) ? State.FETCHING : State.TRAVELING;
        if (state == State.TRAVELING) navigateToBase(entity);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        WriterJob job = getWriterJob(entity);
        if (job == null || entity.getBrain().getMemory(CftMemoryModuleType.MUST_WRITE.get()).isEmpty()) {
            return false;
        }
        return needsFetch(entity, job) || (basePos(entity) != null && !isAtBase(entity));
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        WriterJob job = getWriterJob(entity);
        if (job == null) return;

        switch (state) {
            case FETCHING -> tickFetching(level, entity, job);
            case TRAVELING -> tickTraveling(entity);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
    }

    private void tickFetching(ServerLevel level, XoonglinEntity entity, WriterJob job) {
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        BlockPos base = basePos(entity);
        if (base == null) {
            waitTicks = NO_INPUT_RETRY_COOLDOWN;
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(base))
                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            entity.getNavigation().stop();

            List<Container> containers = job.baseContainers(level, entity);
            if (containers.isEmpty() || !ContainerUtil.hasAllIngredients(containers, List.of(job.getInput().get()))) {
                waitTicks = NO_INPUT_RETRY_COOLDOWN;
                return;
            }

            takeInput(entity, containers, job);
            containers.forEach(Container::setChanged);
            if (!needsFetch(entity, job)) {
                state = State.TRAVELING;
                repathTimer = 0;
            }
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, base);
        }
    }

    private void tickTraveling(XoonglinEntity entity) {
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateToBase(entity);
        }
    }

    private void takeInput(XoonglinEntity entity, List<Container> containers, WriterJob job) {
        job.getInput().ifPresent(item -> {
            int wanted = item.quantity();
            for (Container container : containers) {
                for (int i = 0; i < container.getContainerSize() && wanted > 0; i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty() || !item.ingredient().test(stack)) continue;
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
        });
    }

    private boolean needsFetch(XoonglinEntity entity, WriterJob job) {
        return job.getInput().isPresent() && !job.hasEnoughInput(entity);
    }

    private boolean isAtBase(XoonglinEntity entity) {
        BlockPos base = basePos(entity);
        return base != null && base.closerToCenterThan(entity.position(), CftConfig.HOME_WORK_RADIUS.get());
    }

    private BlockPos basePos(XoonglinEntity entity) {
        WriterJob job = getWriterJob(entity);
        if (job == null) return null;
        if (job.getRequiredStructureType() != null) {
            return entity.getAssignedStructurePos(job.getRequiredStructureType());
        }
        return entity.getHome() != null ? entity.getHome().getEntrance() : null;
    }

    private void navigateToBase(XoonglinEntity entity) {
        BlockPos base = basePos(entity);
        if (base != null) {
            navigateTo(entity, base);
        }
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private WriterJob getWriterJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof WriterJob w ? w : null;
    }
}
