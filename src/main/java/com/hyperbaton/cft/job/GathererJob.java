package com.hyperbaton.cft.job;

import com.hyperbaton.cft.util.RegistryEntries;
import com.hyperbaton.cft.CftConfig;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class GathererJob extends Job {

    public static final Codec<GathererJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            propertiesCodec(),
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.INT.fieldOf("gather_radius").forGetter(j -> j.gatherRadius),
            RegistryEntries.codec(Registries.BLOCK).fieldOf("block").forGetter(j -> j.block)
    ).apply(inst, GathererJob::new));

    private final double hoursPerDay;
    private final int gatherRadius;
    private final RegistryEntries<Block> block;



    public GathererJob(Properties properties, double hoursPerDay, int gatherRadius, RegistryEntries<Block> block) {
        super(properties);
        this.hoursPerDay = hoursPerDay;
        this.gatherRadius = gatherRadius;
        this.block = block;
    }

    public boolean matchesBlock(BlockState state) {
        return block.contains(state.getBlockHolder());
    }

    public int getGatherRadius() {
        return gatherRadius;
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

        if (JobUtil.isAtHome(xoonglin, CftConfig.HOME_WORK_RADIUS.get()) && canWork(xoonglin)) {
            state.workedTicksToday++;
        }

        boolean hasHome = xoonglin.getHome() != null && xoonglin.getHome().getEntrance() != null;
        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        if (hasHome && state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_GATHER.get(), Boolean.TRUE);
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_GATHER.get());
        }
    }

    @Override
    public MemoryModuleType<Boolean> getWorkMemory() {
        return CftMemoryModuleType.MUST_GATHER.get();
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_GATHER.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
        boolean atHome = JobUtil.isAtHome(xoonglin, gatherRadius);
        boolean canDoWork = canWork(xoonglin);
        boolean doneForDay = state.workedTicksToday >= neededTicks;

        JobStatus status;
        if (!canDoWork) {
            status = cantWorkStatus(xoonglin);
        } else if (doneForDay) {
            status = JobStatus.RESTING;
        } else if (atHome) {
            status = JobUtil.workingStatus(xoonglin);
        } else {
            status = JobStatus.TRAVELING;
        }

        List<JobDisplayEntry> entries = new ArrayList<>();
        entries.add(JobDisplayEntry.progress("gui.cft.job_today", state.workedTicksToday, neededTicks,
                JobUtil.formatWorkTime(state.workedTicksToday, hoursPerDay)));
        block.ids().stream().findFirst()
                .ifPresent(id -> entries.add(JobDisplayEntry.item("gui.cft.job_gathering", id, 0)));

        return new JobInfoData(status, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.GATHERER_JOB.get();
    }
}
