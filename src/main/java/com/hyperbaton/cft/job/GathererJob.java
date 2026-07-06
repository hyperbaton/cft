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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GathererJob extends Job {

    public static final Codec<GathererJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.INT.fieldOf("gather_radius").forGetter(j -> j.gatherRadius),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block").forGetter(j -> Optional.ofNullable(j.block)),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("block_tag").forGetter(j -> Optional.ofNullable(j.blockTag)),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness)
    ).apply(inst, GathererJob::new));

    private final double hoursPerDay;
    private final int gatherRadius;
    private final Block block;
    private final TagKey<Block> blockTag;



    public GathererJob(double hoursPerDay, int gatherRadius, Optional<Block> block,
                       Optional<TagKey<Block>> blockTag, List<String> requiredNeeds, double minHappiness) {
        super(requiredNeeds, minHappiness);
        this.hoursPerDay = hoursPerDay;
        this.gatherRadius = gatherRadius;
        this.block = block.orElse(null);
        this.blockTag = blockTag.orElse(null);
    }

    public boolean matchesBlock(BlockState state) {
        if (block != null && state.is(block)) return true;
        if (blockTag != null && state.is(blockTag)) return true;
        return false;
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
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_GATHER.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
        boolean atHome = JobUtil.isAtHome(xoonglin, gatherRadius);
        boolean canDoWork = canWork(xoonglin);
        boolean doneForDay = state.workedTicksToday >= neededTicks;

        String statusKey;
        int statusColor;
        if (!canDoWork) {
            statusKey = "gui.cft.job_status.cant_work";
            statusColor = 0xDD4040;
        } else if (doneForDay) {
            statusKey = "gui.cft.job_status.resting";
            statusColor = 0xDDAA00;
        } else if (atHome) {
            statusKey = "gui.cft.job_status.working";
            statusColor = 0x40AA40;
        } else {
            statusKey = "gui.cft.job_status.traveling";
            statusColor = 0x4080DD;
        }

        List<JobDisplayEntry> entries = new ArrayList<>();
        entries.add(JobDisplayEntry.progress("gui.cft.job_today", state.workedTicksToday, neededTicks,
                JobUtil.formatWorkTime(state.workedTicksToday, hoursPerDay)));
        if (block != null) {
            entries.add(JobDisplayEntry.item("gui.cft.job_gathering",
                    BuiltInRegistries.BLOCK.getKey(block), 0));
        }

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.GATHERER_JOB.get();
    }
}
