package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.Job;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A behavior that does a job's work, in the WORK activity. It starts when the job sets its MUST_*
 * memory and keeps going while that memory is there and the Xoonglin is still in the WORK activity,
 * so anything that takes it away from work (an errand, a ritual, the end of its shift) stops it.
 * While it runs, the step of its work it's on is shown in the job tab.
 *
 * @param <J> the job type it does the work of
 */
public abstract class JobBehavior<J extends Job> extends Behavior<XoonglinEntity> {

    private final MemoryModuleType<Boolean> mustWorkMemory;
    private final Class<J> jobClass;

    /**
     * @param mustWorkMemory the memory the job sets while there's work to do
     * @param jobClass       the job type, to look it up with {@link #getJob(XoonglinEntity)}
     * @param maxDuration    how long it runs at most before restarting, in ticks
     */
    protected JobBehavior(MemoryModuleType<Boolean> mustWorkMemory, Class<J> jobClass, int maxDuration) {
        super(Map.of(mustWorkMemory, MemoryStatus.VALUE_PRESENT), maxDuration);
        this.mustWorkMemory = mustWorkMemory;
        this.jobClass = jobClass;
    }

    @Override
    protected final boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().isActive(Activity.WORK)
                && entity.getBrain().hasMemoryValue(mustWorkMemory)
                && canKeepWorking(level, entity, gameTime);
    }

    /** Further conditions for the behavior to keep running, besides being at work. */
    protected boolean canKeepWorking(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return true;
    }

    @Override
    protected final void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        WorkStep step = workStep();
        if (step != null) {
            entity.getBrain().setMemory(CftMemoryModuleType.WORK_STEP.get(), step);
        }
        tickWork(level, entity, gameTime);
    }

    protected abstract void tickWork(ServerLevel level, XoonglinEntity entity, long gameTime);

    /** The step of its work it's on, shown in the job tab; null to show none. */
    @Nullable
    protected WorkStep workStep() {
        return null;
    }

    @Override
    protected final void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getBrain().eraseMemory(CftMemoryModuleType.WORK_STEP.get());
        stopWork(level, entity, gameTime);
    }

    protected void stopWork(ServerLevel level, XoonglinEntity entity, long gameTime) {
    }

    /** The Xoonglin's job, or null if it doesn't have one of this type. */
    @Nullable
    protected J getJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return jobClass.isInstance(job) ? jobClass.cast(job) : null;
    }
}
