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
 * Works at an enchanting structure, taking an item from the structure's container and
 * applying an enchantment from its repertoire, then depositing it back into the same
 * container. Which enchantment (and level) is picked is resolved at work time by
 * EnchantBehavior, based on what's already on the item and what its repertoire offers.
 */
public class EnchanterJob extends Job {

    public static final Codec<EnchanterJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.STRING.fieldOf("required_structure").forGetter(j -> j.requiredStructure),
            INGREDIENT_CODEC.fieldOf("input").forGetter(j -> j.input),
            EnchantmentOption.CODEC.listOf().fieldOf("repertoire").forGetter(EnchanterJob::getRepertoire),
            Codec.INT.optionalFieldOf("enchanting_time", 200).forGetter(EnchanterJob::getEnchantingTime),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness),
            Codec.BOOL.optionalFieldOf("available_to_babies", false).forGetter(Job::isAvailableToBabies),
            Codec.BOOL.optionalFieldOf("available_to_adults", true).forGetter(Job::isAvailableToAdults)
    ).apply(inst, EnchanterJob::new));

    private final double hoursPerDay;
    private final String requiredStructure;
    private final Ingredient input;
    private final List<EnchantmentOption> repertoire;
    private final int enchantingTime;

    public EnchanterJob(double hoursPerDay, String requiredStructure, Ingredient input,
                        List<EnchantmentOption> repertoire, int enchantingTime, List<String> requiredNeeds,
                        double minHappiness, boolean availableToBabies, boolean availableToAdults) {
        super(requiredNeeds, minHappiness, availableToBabies, availableToAdults);
        this.hoursPerDay = hoursPerDay;
        this.requiredStructure = requiredStructure;
        this.input = input;
        this.repertoire = List.copyOf(repertoire);
        this.enchantingTime = enchantingTime;
    }

    public Ingredient getInput() {
        return input;
    }

    public List<EnchantmentOption> getRepertoire() {
        return repertoire;
    }

    public int getEnchantingTime() {
        return enchantingTime;
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
            brain.eraseMemory(CftMemoryModuleType.MUST_ENCHANT.get());
        } else if (state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_ENCHANT.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_ENCHANT.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    private boolean isAtStructure(XoonglinEntity xoonglin, BlockPos structurePos) {
        return structurePos.closerToCenterThan(xoonglin.position(), CftConfig.HOME_WORK_RADIUS.get());
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_ENCHANT.get());
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

        ItemStack[] inputMatches = input.getItems();
        if (inputMatches.length > 0) {
            entries.add(JobDisplayEntry.item("gui.cft.job_inputs",
                    BuiltInRegistries.ITEM.getKey(inputMatches[0].getItem()), 1));
        }

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.ENCHANTER_JOB.get();
    }
}
