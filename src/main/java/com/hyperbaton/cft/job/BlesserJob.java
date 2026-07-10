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
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Blesses nearby Xoonglins (and optionally the leader) by spending items, applying a
 * set of beneficial effects. If a required structure is set, the blesser works from it,
 * treating targets within a radius of the structure and taking supplies from its
 * containers; otherwise it works from its home. Supplies are carried in the blesser's
 * inventory and restocked from the base.
 */
public class BlesserJob extends Job {

    public static final Codec<BlesserJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.INT.optionalFieldOf("radius", 24).forGetter(BlesserJob::getRadius),
            Codec.STRING.optionalFieldOf("required_structure", "").forGetter(j -> j.requiredStructure),
            ItemQuantity.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(BlesserJob::getItems),
            EffectApplication.CODEC.listOf().fieldOf("effects").forGetter(BlesserJob::getEffects),
            Codec.INT.optionalFieldOf("cooldown", 100).forGetter(BlesserJob::getCooldown),
            Codec.INT.optionalFieldOf("doses_per_fetch", 16).forGetter(BlesserJob::getDosesPerFetch),
            Codec.BOOL.optionalFieldOf("bless_player", false).forGetter(BlesserJob::isBlessPlayer),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness),
            Codec.BOOL.optionalFieldOf("available_to_babies", false).forGetter(Job::isAvailableToBabies),
            Codec.BOOL.optionalFieldOf("available_to_adults", true).forGetter(Job::isAvailableToAdults)
    ).apply(inst, BlesserJob::new));

    private final double hoursPerDay;
    private final int radius;
    private final String requiredStructure;
    private final List<ItemQuantity> items;
    private final List<EffectApplication> effects;
    private final int cooldown;
    private final int dosesPerFetch;
    private final boolean blessPlayer;

    public BlesserJob(double hoursPerDay, int radius, String requiredStructure, List<ItemQuantity> items,
                      List<EffectApplication> effects, int cooldown, int dosesPerFetch, boolean blessPlayer,
                      List<String> requiredNeeds, double minHappiness,
                      boolean availableToBabies, boolean availableToAdults) {
        super(requiredNeeds, minHappiness, availableToBabies, availableToAdults);
        this.hoursPerDay = hoursPerDay;
        this.radius = radius;
        this.requiredStructure = requiredStructure;
        this.items = List.copyOf(items);
        this.effects = List.copyOf(effects);
        this.cooldown = cooldown;
        this.dosesPerFetch = dosesPerFetch;
        this.blessPlayer = blessPlayer;
    }

    public int getRadius() {
        return radius;
    }

    public List<ItemQuantity> getItems() {
        return items;
    }

    public List<EffectApplication> getEffects() {
        return effects;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getDosesPerFetch() {
        return dosesPerFetch;
    }

    public boolean isBlessPlayer() {
        return blessPlayer;
    }

    @Override
    public String getRequiredStructureType() {
        return requiredStructure.isEmpty() ? null : requiredStructure;
    }

    /** True if the inventory holds enough items for one blessing (always true when free). */
    public boolean hasDose(XoonglinEntity blesser) {
        SimpleContainer inventory = blesser.getInventory();
        for (ItemQuantity item : items) {
            int found = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && item.ingredient().test(stack)) {
                    found += stack.getCount();
                }
            }
            if (found < item.quantity()) {
                return false;
            }
        }
        return true;
    }

    /** Removes one blessing's worth of items from the blesser's inventory. */
    public void consumeDose(XoonglinEntity blesser) {
        SimpleContainer inventory = blesser.getInventory();
        for (ItemQuantity item : items) {
            int remaining = item.quantity();
            for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && item.ingredient().test(stack)) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }
        }
    }

    public boolean isDoseItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return items.stream().anyMatch(item -> item.ingredient().test(stack));
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
        boolean hasBase = !requiredStructure.isEmpty()
                ? xoonglin.getAssignedStructurePos(requiredStructure) != null
                : (xoonglin.getHome() != null && xoonglin.getHome().getEntrance() != null);

        if (needsStructure) {
            brain.setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), requiredStructure);
            brain.eraseMemory(CftMemoryModuleType.MUST_BLESS.get());
        } else if (hasBase && state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_BLESS.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
            state.workedTicksToday++;
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_BLESS.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_BLESS.get());
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
        for (ItemQuantity item : items) {
            ItemStack[] matches = item.ingredient().getItems();
            if (matches.length == 0) continue;
            int carried = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (isDoseItem(stack)) carried += stack.getCount();
            }
            entries.add(JobDisplayEntry.item("gui.cft.job_supplies",
                    BuiltInRegistries.ITEM.getKey(matches[0].getItem()), carried));
            break;
        }

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.BLESSER_JOB.get();
    }
}
