package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.JobDisplayEntry;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.util.JobUtil;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.hyperbaton.cft.need.codec.CftCodec.INGREDIENT_CODEC;

/**
 * Fishes from a body of blocks (water by default) of at least a minimum size within a
 * radius. If a required structure is set, the fisher stands on it, as close to the
 * water as possible; otherwise it stands on the shore. Every catch interval it rolls
 * the weighted catch list — weights must sum 1 or less, and the missing probability is
 * the chance that nothing bites. Catches are carried in the inventory and deposited in
 * the structure's containers (or at home if there are none) at the end of the work day.
 */
public class FisherJob extends Job {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<FisherJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.INT.optionalFieldOf("radius", 32).forGetter(FisherJob::getRadius),
            Codec.STRING.optionalFieldOf("required_structure", "").forGetter(j -> j.requiredStructure),
            BodyBlock.CODEC.listOf().optionalFieldOf("body_blocks", List.of(BodyBlock.WATER))
                    .forGetter(FisherJob::getBodyBlocks),
            Codec.INT.optionalFieldOf("min_body_size", 20).forGetter(FisherJob::getMinBodySize),
            Codec.INT.optionalFieldOf("catch_interval", 300).forGetter(FisherJob::getCatchInterval),
            Catch.CODEC.listOf().fieldOf("catches").forGetter(FisherJob::getCatches),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness)
    ).apply(inst, FisherJob::new));

    /**
     * A block (or block tag) that can form the fished body.
     */
    public record BodyBlock(Optional<Block> block, Optional<TagKey<Block>> tagBlock) {
        public static final BodyBlock WATER = new BodyBlock(Optional.of(Blocks.WATER), Optional.empty());

        public static final Codec<BodyBlock> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block").forGetter(BodyBlock::block),
                TagKey.codec(Registries.BLOCK).optionalFieldOf("tagBlock").forGetter(BodyBlock::tagBlock)
        ).apply(inst, BodyBlock::new));

        public boolean matches(BlockState state) {
            if (block.isPresent() && state.is(block.get())) return true;
            return tagBlock.isPresent() && state.is(tagBlock.get());
        }
    }

    /**
     * A possible catch with its probability per roll.
     */
    public record Catch(Ingredient item, double weight, int count) {
        public static final Codec<Catch> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                INGREDIENT_CODEC.fieldOf("item").forGetter(Catch::item),
                Codec.DOUBLE.fieldOf("weight").forGetter(Catch::weight),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Catch::count)
        ).apply(inst, Catch::new));
    }

    private final double hoursPerDay;
    private final int radius;
    private final String requiredStructure;
    private final List<BodyBlock> bodyBlocks;
    private final int minBodySize;
    private final int catchInterval;
    private final List<Catch> catches;

    public FisherJob(double hoursPerDay, int radius, String requiredStructure, List<BodyBlock> bodyBlocks,
                     int minBodySize, int catchInterval, List<Catch> catches, List<String> requiredNeeds,
                     double minHappiness) {
        super(requiredNeeds, minHappiness);
        this.hoursPerDay = hoursPerDay;
        this.radius = radius;
        this.requiredStructure = requiredStructure;
        this.bodyBlocks = List.copyOf(bodyBlocks);
        this.minBodySize = minBodySize;
        this.catchInterval = catchInterval;
        this.catches = List.copyOf(catches);
        validateCatches();
    }

    private void validateCatches() {
        double totalWeight = catches.stream().mapToDouble(Catch::weight).sum();
        if (totalWeight > 1.0 + 1e-9) {
            LOGGER.error("Fisher job: catch weights sum {} but must not exceed 1.0", totalWeight);
            throw new IllegalArgumentException(
                    String.format("Fisher job: catch weights sum %.3f but must not exceed 1.0", totalWeight));
        }
    }

    public int getRadius() {
        return radius;
    }

    public List<BodyBlock> getBodyBlocks() {
        return bodyBlocks;
    }

    public int getMinBodySize() {
        return minBodySize;
    }

    public int getCatchInterval() {
        return catchInterval;
    }

    public List<Catch> getCatches() {
        return catches;
    }

    @Override
    public String getRequiredStructureType() {
        return requiredStructure.isEmpty() ? null : requiredStructure;
    }

    public boolean isBodyBlock(BlockState state) {
        return bodyBlocks.stream().anyMatch(bodyBlock -> bodyBlock.matches(state));
    }

    /**
     * Rolls the weighted catch list. Returns an empty stack when nothing bites
     * (weights summing less than 1 leave that probability open).
     */
    public ItemStack rollCatch(RandomSource random) {
        double roll = random.nextDouble();
        double cumulative = 0.0;
        for (Catch entry : catches) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                ItemStack[] matches = entry.item().getItems();
                if (matches.length == 0) return ItemStack.EMPTY;
                ItemStack stack = matches[0].copy();
                stack.setCount(entry.count());
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean isCatchItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return catches.stream().anyMatch(entry -> entry.item().test(stack));
    }

    public boolean hasCatchItems(XoonglinEntity xoonglin) {
        SimpleContainer inventory = xoonglin.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isCatchItem(inventory.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    public int getNeededTicks() {
        return (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
    }

    public boolean isQuotaDone(JobState state) {
        return state.workedTicksToday >= getNeededTicks();
    }

    @Override
    public void tick(XoonglinEntity xoonglin, JobState state) {
        Level level = xoonglin.level();
        if (level.isClientSide) return;

        long dayIndex = Math.floorDiv(level.getDayTime(), 24000L);
        int neededTicks = getNeededTicks();

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
            brain.eraseMemory(CftMemoryModuleType.MUST_FISH.get());
        } else if (state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_FISH.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
            state.workedTicksToday++;
        } else if (hasCatchItems(xoonglin)) {
            // Work is over (or blocked), but the day's catch must still be delivered
            brain.setMemory(CftMemoryModuleType.MUST_FISH.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_FISH.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_FISH.get());
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = getNeededTicks();
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
        int carried = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isCatchItem(stack)) carried += stack.getCount();
        }
        if (carried > 0) {
            entries.add(JobDisplayEntry.progress("gui.cft.job_carrying", carried, 0));
        }

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.FISHER_JOB.get();
    }
}
