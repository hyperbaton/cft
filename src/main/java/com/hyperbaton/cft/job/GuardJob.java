package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.JobDisplayEntry;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.network.JobStatus;
import com.hyperbaton.cft.util.JobUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class GuardJob extends Job {

    private static final JobStatus FIGHTING = JobStatus.attention("gui.cft.job_status.fighting");
    private static final JobStatus PATROLLING = JobStatus.active("gui.cft.job_status.patrolling");

    public static final Codec<GuardJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            propertiesCodec(),
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.INT.fieldOf("patrol_radius").forGetter(j -> j.patrolRadius),
            Codec.INT.optionalFieldOf("detection_radius", 16).forGetter(j -> j.detectionRadius)
    ).apply(inst, GuardJob::new));

    private final double hoursPerDay;
    private final int patrolRadius;
    private final int detectionRadius;



    public GuardJob(Properties properties, double hoursPerDay, int patrolRadius, int detectionRadius) {
        super(properties);
        this.hoursPerDay = hoursPerDay;
        this.patrolRadius = patrolRadius;
        this.detectionRadius = detectionRadius;
    }

    public int getPatrolRadius() {
        return patrolRadius;
    }

    public int getDetectionRadius() {
        return detectionRadius;
    }

    @Override
    public void tick(XoonglinEntity xoonglin, JobState state) {
        Level level = xoonglin.level();
        if (level.isClientSide) return;

        long dayIndex = Math.floorDiv(level.getDayTime(), 24000L);
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);

        if (state.lastDayIndex == Long.MIN_VALUE) {
            state.lastDayIndex = dayIndex;
        } else if (dayIndex != state.lastDayIndex) {
            boolean metQuota = state.workedTicksToday >= neededTicks;
            if (metQuota) {
                state.consecutiveDaysWorked++;
            } else {
                state.consecutiveDaysWorked = 0;
            }
            state.workedTicksToday = 0;
            state.lastDayIndex = dayIndex;
        }

        boolean nearHome = JobUtil.isAtHome(xoonglin, patrolRadius);
        if (nearHome && canWork(xoonglin)) {
            state.workedTicksToday++;
        }

        boolean hasHome = xoonglin.getHome() != null && xoonglin.getHome().getEntrance() != null;
        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        if (hasHome && state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_GUARD.get(), Boolean.TRUE);
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_GUARD.get());
        }
    }

    @Override
    public MemoryModuleType<Boolean> getWorkMemory() {
        return CftMemoryModuleType.MUST_GUARD.get();
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_GUARD.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
        boolean nearHome = JobUtil.isAtHome(xoonglin, patrolRadius);
        boolean canDoWork = canWork(xoonglin);
        boolean doneForDay = state.workedTicksToday >= neededTicks;
        boolean fighting = xoonglin.getTarget() != null && xoonglin.getTarget().isAlive();

        JobStatus status;
        if (!canDoWork) {
            status = cantWorkStatus(xoonglin);
        } else if (doneForDay) {
            status = JobStatus.RESTING;
        } else if (fighting) {
            status = FIGHTING;
        } else if (nearHome) {
            status = PATROLLING;
        } else {
            status = JobStatus.TRAVELING;
        }

        List<JobDisplayEntry> entries = new ArrayList<>();
        entries.add(JobDisplayEntry.progress("gui.cft.job_today", state.workedTicksToday, neededTicks,
                JobUtil.formatWorkTime(state.workedTicksToday, hoursPerDay)));

        return new JobInfoData(status, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.GUARD_JOB.get();
    }
}
