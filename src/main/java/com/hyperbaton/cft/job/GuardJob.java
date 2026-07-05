package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.JobDisplayEntry;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.util.JobUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class GuardJob extends Job {

    public static final Codec<GuardJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.INT.fieldOf("patrol_radius").forGetter(j -> j.patrolRadius),
            Codec.INT.optionalFieldOf("detection_radius", 16).forGetter(j -> j.detectionRadius),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds)
    ).apply(inst, GuardJob::new));

    private final double hoursPerDay;
    private final int patrolRadius;
    private final int detectionRadius;



    public GuardJob(double hoursPerDay, int patrolRadius, int detectionRadius, List<String> requiredNeeds) {
        super(requiredNeeds);
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

        String statusKey;
        int statusColor;
        if (!canDoWork) {
            statusKey = "gui.cft.job_status.cant_work";
            statusColor = 0xDD4040;
        } else if (doneForDay) {
            statusKey = "gui.cft.job_status.resting";
            statusColor = 0xDDAA00;
        } else if (fighting) {
            statusKey = "gui.cft.job_status.fighting";
            statusColor = 0xDD4040;
        } else if (nearHome) {
            statusKey = "gui.cft.job_status.patrolling";
            statusColor = 0x40AA40;
        } else {
            statusKey = "gui.cft.job_status.traveling";
            statusColor = 0x4080DD;
        }

        List<JobDisplayEntry> entries = new ArrayList<>();
        entries.add(JobDisplayEntry.progress("gui.cft.job_today", state.workedTicksToday, neededTicks,
                JobUtil.formatWorkTime(state.workedTicksToday, hoursPerDay)));

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.GUARD_JOB.get();
    }
}
