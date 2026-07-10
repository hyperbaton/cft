package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.JobDisplayEntry;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.util.JobUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class BuilderJob extends Job {

    public static final Codec<BuilderJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.STRING.optionalFieldOf("required_structure", "").forGetter(j -> j.requiredStructure),
            Codec.STRING.fieldOf("storage_structure").forGetter(j -> j.storageStructure),
            Codec.INT.optionalFieldOf("build_radius", 64).forGetter(j -> j.buildRadius),
            Codec.INT.optionalFieldOf("build_speed", 20).forGetter(j -> j.buildSpeed),
            Codec.STRING.listOf().fieldOf("buildable_structures").forGetter(j -> j.buildableStructures),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness),
            Codec.BOOL.optionalFieldOf("available_to_babies", false).forGetter(Job::isAvailableToBabies),
            Codec.BOOL.optionalFieldOf("available_to_adults", true).forGetter(Job::isAvailableToAdults)
    ).apply(inst, BuilderJob::new));

    private final double hoursPerDay;
    private final String requiredStructure;
    private final String storageStructure;
    private final int buildRadius;
    private final int buildSpeed;
    private final List<String> buildableStructures;

    public BuilderJob(double hoursPerDay, String requiredStructure, String storageStructure,
                      int buildRadius, int buildSpeed, List<String> buildableStructures,
                      List<String> requiredNeeds, double minHappiness,
                      boolean availableToBabies, boolean availableToAdults) {
        super(requiredNeeds, minHappiness, availableToBabies, availableToAdults);
        this.hoursPerDay = hoursPerDay;
        this.requiredStructure = requiredStructure;
        this.storageStructure = storageStructure;
        this.buildRadius = buildRadius;
        this.buildSpeed = buildSpeed;
        this.buildableStructures = List.copyOf(buildableStructures);
    }

    public String getStorageStructure() {
        return storageStructure;
    }

    public int getBuildRadius() {
        return buildRadius;
    }

    public int getBuildSpeed() {
        return buildSpeed;
    }

    public List<String> getBuildableStructures() {
        return buildableStructures;
    }

    @Override
    public String getRequiredStructureType() {
        return requiredStructure.isEmpty() ? null : requiredStructure;
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

        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        boolean needsStructure = !requiredStructure.isEmpty()
                && xoonglin.getAssignedStructurePos(requiredStructure) == null;

        if (needsStructure) {
            brain.setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), requiredStructure);
            brain.eraseMemory(CftMemoryModuleType.MUST_BUILD.get());
        } else if (state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_BUILD.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
            state.workedTicksToday++;
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_BUILD.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_BUILD.get());
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
        boolean canDoWork = canWork(xoonglin);
        boolean doneForDay = state.workedTicksToday >= neededTicks;

        boolean needsStructure = !requiredStructure.isEmpty()
                && xoonglin.getAssignedStructurePos(requiredStructure) == null;

        String statusKey;
        int statusColor;
        if (needsStructure) {
            statusKey = "gui.cft.job_status.no_structure";
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

        SimpleContainer inventory = xoonglin.getInventory();
        int totalCarried = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) totalCarried += stack.getCount();
        }
        if (totalCarried > 0) {
            entries.add(JobDisplayEntry.progress("gui.cft.job_carrying_blocks", totalCarried, 0));
        }

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.BUILDER_JOB.get();
    }
}
