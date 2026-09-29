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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Xoonglin fells whole natural trees around its base (a lumber camp, or its home if the job
 * has no required structure), breaking their logs and leaves, replanting saplings and storing
 * the wood at its base. It needs an axe, which it holds (or carries in its inventory if an
 * equipment need takes its main hand) and wears out.
 */
public class LumberjackJob extends Job {

    private static final JobStatus NEEDS_AXE = JobStatus.attention("gui.cft.job_status.needs_axe");

    public static final Codec<LumberjackJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.INT.optionalFieldOf("chop_radius", 16).forGetter(LumberjackJob::getChopRadius),
            Codec.STRING.optionalFieldOf("required_structure").forGetter(j -> Optional.ofNullable(j.requiredStructure)),
            Codec.INT.optionalFieldOf("max_tree_size", 128).forGetter(LumberjackJob::getMaxTreeSize),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness),
            Codec.BOOL.optionalFieldOf("available_to_babies", false).forGetter(Job::isAvailableToBabies),
            Codec.BOOL.optionalFieldOf("available_to_adults", true).forGetter(Job::isAvailableToAdults)
    ).apply(inst, LumberjackJob::new));

    private final double hoursPerDay;
    private final int chopRadius;
    private final String requiredStructure;
    private final int maxTreeSize;

    public LumberjackJob(double hoursPerDay, int chopRadius, Optional<String> requiredStructure, int maxTreeSize,
                         List<String> requiredNeeds, double minHappiness, boolean availableToBabies,
                         boolean availableToAdults) {
        super(requiredNeeds, minHappiness, availableToBabies, availableToAdults);
        this.hoursPerDay = hoursPerDay;
        this.chopRadius = chopRadius;
        this.requiredStructure = requiredStructure.orElse(null);
        this.maxTreeSize = maxTreeSize;
    }

    /** How far from its base the lumberjack looks for trees. */
    public int getChopRadius() {
        return chopRadius;
    }

    /** Log clusters bigger than this are not felled. */
    public int getMaxTreeSize() {
        return maxTreeSize;
    }

    @Override
    public String getRequiredStructureType() {
        return requiredStructure;
    }

    /** The key block of its lumber camp or, without a required structure, its home entrance. */
    public BlockPos getBasePos(XoonglinEntity xoonglin) {
        if (requiredStructure != null) {
            return xoonglin.getAssignedStructurePos(requiredStructure);
        }
        return xoonglin.getHome() != null ? xoonglin.getHome().getEntrance() : null;
    }

    @Override
    public void tick(XoonglinEntity xoonglin, JobState state) {
        Level level = xoonglin.level();
        if (level.isClientSide) return;

        JobUtil.equipTool(xoonglin, ItemTags.AXES);

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

        BlockPos basePos = getBasePos(xoonglin);
        // Work only counts while it has an axe and is within its area
        if (basePos != null && canWork(xoonglin) && !JobUtil.findTool(xoonglin, ItemTags.AXES).isEmpty()
                && basePos.closerToCenterThan(xoonglin.position(), chopRadius + 4)) {
            state.workedTicksToday++;
        }

        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        if (requiredStructure != null && basePos == null) {
            brain.setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), requiredStructure);
            brain.eraseMemory(CftMemoryModuleType.MUST_CHOP.get());
        } else if (basePos != null && state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_CHOP.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_CHOP.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_CHOP.get());
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
        boolean doneForDay = state.workedTicksToday >= neededTicks;
        ItemStack axe = JobUtil.findTool(xoonglin, ItemTags.AXES);

        JobStatus status;
        if (getBasePos(xoonglin) == null) {
            status = JobStatus.NO_STRUCTURE;
        } else if (!canWork(xoonglin)) {
            status = JobStatus.CANT_WORK;
        } else if (doneForDay) {
            status = JobStatus.RESTING;
        } else if (axe.isEmpty()) {
            status = NEEDS_AXE;
        } else {
            status = JobStatus.WORKING;
        }

        List<JobDisplayEntry> entries = new ArrayList<>();
        entries.add(JobDisplayEntry.progress("gui.cft.job_today", state.workedTicksToday, neededTicks,
                JobUtil.formatWorkTime(state.workedTicksToday, hoursPerDay)));
        if (!axe.isEmpty()) {
            entries.add(JobDisplayEntry.progress("gui.cft.job_axe", axe.getMaxDamage() - axe.getDamageValue(),
                    axe.getMaxDamage()));
            entries.add(JobDisplayEntry.item("gui.cft.job_axe", BuiltInRegistries.ITEM.getKey(axe.getItem()), 1));
        }

        return new JobInfoData(status, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.LUMBERJACK_JOB.get();
    }
}
