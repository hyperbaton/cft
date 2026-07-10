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
 * Works at a workshop structure, consuming ingredients from the workshop's container
 * to craft an output that is deposited back into it. Unlike the home artisan, the
 * inputs are explicit: the craft only happens while the workshop container holds them
 * (delivered there, for example, by haulers).
 */
public class CrafterJob extends Job {

    public static final Codec<CrafterJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.STRING.fieldOf("required_structure").forGetter(j -> j.requiredStructure),
            ItemQuantity.CODEC.listOf().fieldOf("ingredients").forGetter(CrafterJob::getIngredients),
            INGREDIENT_CODEC.fieldOf("output").forGetter(j -> j.output),
            Codec.INT.optionalFieldOf("output_count", 1).forGetter(CrafterJob::getOutputCount),
            Codec.INT.optionalFieldOf("crafting_time", 200).forGetter(CrafterJob::getCraftingTime),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness),
            Codec.BOOL.optionalFieldOf("available_to_babies", false).forGetter(Job::isAvailableToBabies),
            Codec.BOOL.optionalFieldOf("available_to_adults", true).forGetter(Job::isAvailableToAdults)
    ).apply(inst, CrafterJob::new));

    private final double hoursPerDay;
    private final String requiredStructure;
    private final List<ItemQuantity> ingredients;
    private final Ingredient output;
    private final int outputCount;
    private final int craftingTime;

    public CrafterJob(double hoursPerDay, String requiredStructure, List<ItemQuantity> ingredients,
                      Ingredient output, int outputCount, int craftingTime, List<String> requiredNeeds,
                      double minHappiness, boolean availableToBabies, boolean availableToAdults) {
        super(requiredNeeds, minHappiness, availableToBabies, availableToAdults);
        this.hoursPerDay = hoursPerDay;
        this.requiredStructure = requiredStructure;
        this.ingredients = List.copyOf(ingredients);
        this.output = output;
        this.outputCount = outputCount;
        this.craftingTime = craftingTime;
    }

    public List<ItemQuantity> getIngredients() {
        return ingredients;
    }

    public int getOutputCount() {
        return outputCount;
    }

    public int getCraftingTime() {
        return craftingTime;
    }

    @Override
    public String getRequiredStructureType() {
        return requiredStructure;
    }

    public ItemStack createOutputStack() {
        ItemStack[] matches = output.getItems();
        if (matches.length == 0) return ItemStack.EMPTY;
        ItemStack stack = matches[0].copy();
        stack.setCount(outputCount);
        return stack;
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
            brain.eraseMemory(CftMemoryModuleType.MUST_CRAFT.get());
        } else if (state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_CRAFT.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_CRAFT.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    private boolean isAtStructure(XoonglinEntity xoonglin, BlockPos structurePos) {
        return structurePos.closerToCenterThan(xoonglin.position(), CftConfig.HOME_WORK_RADIUS.get());
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_CRAFT.get());
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
        boolean canDoWork = canWork(xoonglin);
        boolean doneForDay = state.workedTicksToday >= neededTicks;
        boolean needsStructure = xoonglin.getAssignedStructurePos(requiredStructure) == null;

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

        for (ItemQuantity ingredient : ingredients) {
            ItemStack[] matches = ingredient.ingredient().getItems();
            if (matches.length > 0) {
                entries.add(JobDisplayEntry.item("gui.cft.job_inputs",
                        BuiltInRegistries.ITEM.getKey(matches[0].getItem()), ingredient.quantity()));
            }
        }
        ItemStack[] outputMatches = output.getItems();
        if (outputMatches.length > 0) {
            entries.add(JobDisplayEntry.item("gui.cft.job_output",
                    BuiltInRegistries.ITEM.getKey(outputMatches[0].getItem()), outputCount));
        }

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.CRAFTER_JOB.get();
    }
}
