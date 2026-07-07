package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.JobDisplayEntry;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.util.JobUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Digs a bounded quarry (an open-air-platform structure) layer by layer, top-down. The
 * miner descends into the quarry through a ladder column it maintains, deposits the
 * mined blocks in the quarry's container, and only mines within the structure footprint
 * down to a maximum depth. Ladders are taken from the quarry's container.
 */
public class QuarryMinerJob extends Job {

    public static final Codec<QuarryMinerJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.STRING.fieldOf("required_structure").forGetter(j -> j.requiredStructure),
            Codec.INT.optionalFieldOf("max_depth", 16).forGetter(QuarryMinerJob::getMaxDepth),
            Codec.INT.optionalFieldOf("mine_speed", 20).forGetter(QuarryMinerJob::getMineSpeed),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness)
    ).apply(inst, QuarryMinerJob::new));

    private final double hoursPerDay;
    private final String requiredStructure;
    private final int maxDepth;
    private final int mineSpeed;

    public QuarryMinerJob(double hoursPerDay, String requiredStructure, int maxDepth, int mineSpeed,
                          List<String> requiredNeeds, double minHappiness) {
        super(requiredNeeds, minHappiness);
        this.hoursPerDay = hoursPerDay;
        this.requiredStructure = requiredStructure;
        this.maxDepth = maxDepth;
        this.mineSpeed = mineSpeed;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMineSpeed() {
        return mineSpeed;
    }

    public boolean isQuotaDone(JobState state) {
        return state.workedTicksToday >= (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
    }

    @Override
    public String getRequiredStructureType() {
        return requiredStructure;
    }

    public boolean hasCarriedBlocks(XoonglinEntity xoonglin) {
        SimpleContainer inventory = xoonglin.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() != Items.LADDER) return true;
        }
        return false;
    }

    public int countLadders(XoonglinEntity xoonglin) {
        SimpleContainer inventory = xoonglin.getInventory();
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == Items.LADDER) count += stack.getCount();
        }
        return count;
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
            state.consecutiveDaysWorked = metQuota ? state.consecutiveDaysWorked + 1 : 0;
            state.workedTicksToday = 0;
            state.lastDayIndex = dayIndex;
        }

        Brain<XoonglinEntity> brain = xoonglin.getBrain();
        boolean needsStructure = xoonglin.getAssignedStructurePos(requiredStructure) == null;
        boolean flooded = brain.hasMemoryValue(CftMemoryModuleType.QUARRY_FLOODED.get());
        boolean needsLadders = brain.hasMemoryValue(CftMemoryModuleType.QUARRY_NEEDS_LADDERS.get());

        if (needsStructure) {
            brain.setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), requiredStructure);
            brain.eraseMemory(CftMemoryModuleType.MUST_MINE.get());
        } else if (flooded || needsLadders) {
            // Keep the behavior running so it can detect the condition clearing, but the
            // quota does not advance while the quarry is unworkable
            brain.setMemory(CftMemoryModuleType.MUST_MINE.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        } else if ((state.workedTicksToday < neededTicks || hasCarriedBlocks(xoonglin)) && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_MINE.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
            if (state.workedTicksToday < neededTicks) state.workedTicksToday++;
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_MINE.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_MINE.get());
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
        boolean canDoWork = canWork(xoonglin);
        boolean doneForDay = state.workedTicksToday >= neededTicks;
        boolean needsStructure = xoonglin.getAssignedStructurePos(requiredStructure) == null;
        boolean flooded = xoonglin.getBrain().hasMemoryValue(CftMemoryModuleType.QUARRY_FLOODED.get());
        boolean needsLadders = xoonglin.getBrain().hasMemoryValue(CftMemoryModuleType.QUARRY_NEEDS_LADDERS.get());

        String statusKey;
        int statusColor;
        if (needsStructure) {
            statusKey = "gui.cft.job_status.no_structure";
            statusColor = 0xDD4040;
        } else if (flooded) {
            statusKey = "gui.cft.job_status.quarry_flooded";
            statusColor = 0xDD4040;
        } else if (needsLadders) {
            statusKey = "gui.cft.job_status.needs_ladders";
            statusColor = 0xDD4040;
        } else if (!canDoWork) {
            statusKey = "gui.cft.job_status.cant_work";
            statusColor = 0xDD4040;
        } else if (doneForDay) {
            statusKey = "gui.cft.job_status.resting";
            statusColor = 0xDDAA00;
        } else {
            statusKey = "gui.cft.job_status.working";
            statusColor = 0x40AA40;
        }

        List<JobDisplayEntry> entries = new ArrayList<>();
        entries.add(JobDisplayEntry.progress("gui.cft.job_today", state.workedTicksToday, neededTicks,
                JobUtil.formatWorkTime(state.workedTicksToday, hoursPerDay)));
        entries.add(JobDisplayEntry.item("gui.cft.job_ladders",
                BuiltInRegistries.ITEM.getKey(Items.LADDER), countLadders(xoonglin)));

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.QUARRY_MINER_JOB.get();
    }
}
