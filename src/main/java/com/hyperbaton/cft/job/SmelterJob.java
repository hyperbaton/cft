package com.hyperbaton.cft.job;

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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

import static com.hyperbaton.cft.need.codec.CftCodec.INGREDIENT_CODEC;

/**
 * The Xoonglin tends the vanilla furnaces, smokers and blast furnaces of a workshop: it loads
 * them with things to cook and fuel from the workshop's chests and collects their results into
 * those same chests. The furnaces do the actual cooking, so any vanilla cooking recipe works.
 * With a structure full of smokers and raw food, the same job is a cook.
 */
public class SmelterJob extends Job {

    public static final Codec<SmelterJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.STRING.fieldOf("required_structure").forGetter(j -> j.requiredStructure),
            INGREDIENT_CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(SmelterJob::getInputs),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness),
            Codec.BOOL.optionalFieldOf("available_to_babies", false).forGetter(Job::isAvailableToBabies),
            Codec.BOOL.optionalFieldOf("available_to_adults", true).forGetter(Job::isAvailableToAdults)
    ).apply(inst, SmelterJob::new));

    private final double hoursPerDay;
    private final String requiredStructure;
    private final List<Ingredient> inputs;

    public SmelterJob(double hoursPerDay, String requiredStructure, List<Ingredient> inputs, List<String> requiredNeeds,
                      double minHappiness, boolean availableToBabies, boolean availableToAdults) {
        super(requiredNeeds, minHappiness, availableToBabies, availableToAdults);
        this.hoursPerDay = hoursPerDay;
        this.requiredStructure = requiredStructure;
        this.inputs = List.copyOf(inputs);
    }

    /** What the smelter may put in the furnaces; empty means anything they can cook. */
    public List<Ingredient> getInputs() {
        return inputs;
    }

    public boolean accepts(ItemStack stack) {
        return inputs.isEmpty() || inputs.stream().anyMatch(input -> input.test(stack));
    }

    @Override
    public String getRequiredStructureType() {
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

        BlockPos structurePos = xoonglin.getAssignedStructurePos(requiredStructure);

        if (structurePos != null && isAtStructure(xoonglin, structurePos) && canWork(xoonglin)) {
            state.workedTicksToday++;
        }

        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        if (structurePos == null) {
            brain.setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), requiredStructure);
            brain.eraseMemory(CftMemoryModuleType.MUST_SMELT.get());
        } else if (state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_SMELT.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_SMELT.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    private boolean isAtStructure(XoonglinEntity xoonglin, BlockPos structurePos) {
        return structurePos.closerToCenterThan(xoonglin.position(), CftConfig.HOME_WORK_RADIUS.get());
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_SMELT.get());
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
        boolean canDoWork = canWork(xoonglin);
        boolean doneForDay = state.workedTicksToday >= neededTicks;
        boolean needsStructure = xoonglin.getAssignedStructurePos(requiredStructure) == null;

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

        for (Ingredient input : inputs) {
            ItemStack[] matches = input.getItems();
            if (matches.length > 0) {
                entries.add(JobDisplayEntry.item("gui.cft.job_inputs",
                        BuiltInRegistries.ITEM.getKey(matches[0].getItem()), 1));
            }
        }

        return new JobInfoData(status, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.SMELTER_JOB.get();
    }
}
