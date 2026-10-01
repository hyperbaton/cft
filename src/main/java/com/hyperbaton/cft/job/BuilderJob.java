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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

public class BuilderJob extends Job {

    public static final Codec<BuilderJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            ResourceLocation.CODEC.optionalFieldOf("required_structure").forGetter(j -> Optional.ofNullable(j.requiredStructure)),
            ResourceLocation.CODEC.fieldOf("storage_structure").forGetter(j -> j.storageStructure),
            Codec.INT.optionalFieldOf("build_radius", 64).forGetter(j -> j.buildRadius),
            Codec.INT.optionalFieldOf("build_speed", 20).forGetter(j -> j.buildSpeed),
            ResourceLocation.CODEC.listOf().fieldOf("buildable_structures").forGetter(j -> j.buildableStructures),
            ResourceLocation.CODEC.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness),
            Codec.BOOL.optionalFieldOf("available_to_babies", false).forGetter(Job::isAvailableToBabies),
            Codec.BOOL.optionalFieldOf("available_to_adults", true).forGetter(Job::isAvailableToAdults)
    ).apply(inst, BuilderJob::new));

    private final double hoursPerDay;
    private final ResourceLocation requiredStructure;
    private final ResourceLocation storageStructure;
    private final int buildRadius;
    private final int buildSpeed;
    private final List<ResourceLocation> buildableStructures;

    public BuilderJob(double hoursPerDay, Optional<ResourceLocation> requiredStructure, ResourceLocation storageStructure,
                      int buildRadius, int buildSpeed, List<ResourceLocation> buildableStructures,
                      List<ResourceLocation> requiredNeeds, double minHappiness,
                      boolean availableToBabies, boolean availableToAdults) {
        super(requiredNeeds, minHappiness, availableToBabies, availableToAdults);
        this.hoursPerDay = hoursPerDay;
        this.requiredStructure = requiredStructure.orElse(null);
        this.storageStructure = storageStructure;
        this.buildRadius = buildRadius;
        this.buildSpeed = buildSpeed;
        this.buildableStructures = List.copyOf(buildableStructures);
    }

    public ResourceLocation getStorageStructure() {
        return storageStructure;
    }

    public int getBuildRadius() {
        return buildRadius;
    }

    public int getBuildSpeed() {
        return buildSpeed;
    }

    public List<ResourceLocation> getBuildableStructures() {
        return buildableStructures;
    }

    @Override
    public ResourceLocation getRequiredStructureType() {
        return requiredStructure;
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

        boolean needsStructure = requiredStructure != null
                && xoonglin.getAssignedStructurePos(requiredStructure) == null;

        if (needsStructure) {
            brain.setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), requiredStructure);
            brain.eraseMemory(CftMemoryModuleType.MUST_BUILD.get());
        } else if (state.workedTicksToday < neededTicks && canWork(xoonglin)
                && JobUtil.checkWorkplace(xoonglin, this)) {
            brain.setMemory(CftMemoryModuleType.MUST_BUILD.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
            state.workedTicksToday++;
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_BUILD.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    @Override
    public MemoryModuleType<Boolean> getWorkMemory() {
        return CftMemoryModuleType.MUST_BUILD.get();
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

        boolean needsStructure = requiredStructure != null
                && xoonglin.getAssignedStructurePos(requiredStructure) == null;

        JobStatus status;
        if (needsStructure) {
            status = noStructureStatus();
        } else if (!canDoWork) {
            status = cantWorkStatus(xoonglin);
        } else if (doneForDay) {
            status = JobStatus.RESTING;
        } else {
            status = JobUtil.workingStatus(xoonglin);
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

        return new JobInfoData(status, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.BUILDER_JOB.get();
    }
}
