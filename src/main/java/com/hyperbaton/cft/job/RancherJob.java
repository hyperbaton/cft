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
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Tends the animals of a pasture: shears sheep, milks cows, and feeds pairs of eligible
 * animals to trigger breeding. All three actions are optional and independently
 * configurable; wool and milk are deposited in the pasture's container, and feed is
 * consumed from it.
 */
public class RancherJob extends Job {

    public static final Codec<RancherJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.STRING.fieldOf("required_structure").forGetter(j -> j.requiredStructure),
            ItemQuantity.CODEC.listOf().optionalFieldOf("feed", List.of()).forGetter(RancherJob::getFeed),
            Codec.BOOL.optionalFieldOf("shear", true).forGetter(RancherJob::isShearEnabled),
            Codec.BOOL.optionalFieldOf("milk", true).forGetter(RancherJob::isMilkEnabled),
            Codec.INT.optionalFieldOf("action_cooldown", 200).forGetter(RancherJob::getActionCooldown),
            Codec.INT.optionalFieldOf("doses_per_fetch", 16).forGetter(RancherJob::getDosesPerFetch),
            Codec.INT.optionalFieldOf("buckets_per_fetch", 4).forGetter(RancherJob::getBucketsPerFetch),
            Codec.INT.optionalFieldOf("milk_regen_ticks", 6000).forGetter(RancherJob::getMilkRegenTicks),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness),
            Codec.BOOL.optionalFieldOf("available_to_babies", false).forGetter(Job::isAvailableToBabies),
            Codec.BOOL.optionalFieldOf("available_to_adults", true).forGetter(Job::isAvailableToAdults)
    ).apply(inst, RancherJob::new));

    private final double hoursPerDay;
    private final String requiredStructure;
    private final List<ItemQuantity> feed;
    private final boolean shearEnabled;
    private final boolean milkEnabled;
    private final int actionCooldown;
    private final int dosesPerFetch;
    private final int bucketsPerFetch;
    private final int milkRegenTicks;

    public RancherJob(double hoursPerDay, String requiredStructure, List<ItemQuantity> feed,
                      boolean shearEnabled, boolean milkEnabled, int actionCooldown,
                      int dosesPerFetch, int bucketsPerFetch, int milkRegenTicks, List<String> requiredNeeds,
                      double minHappiness, boolean availableToBabies, boolean availableToAdults) {
        super(requiredNeeds, minHappiness, availableToBabies, availableToAdults);
        this.hoursPerDay = hoursPerDay;
        this.requiredStructure = requiredStructure;
        this.feed = List.copyOf(feed);
        this.shearEnabled = shearEnabled;
        this.milkEnabled = milkEnabled;
        this.actionCooldown = actionCooldown;
        this.dosesPerFetch = dosesPerFetch;
        this.bucketsPerFetch = bucketsPerFetch;
        this.milkRegenTicks = milkRegenTicks;
    }

    public List<ItemQuantity> getFeed() {
        return feed;
    }

    public boolean isShearEnabled() {
        return shearEnabled;
    }

    public boolean isMilkEnabled() {
        return milkEnabled;
    }

    public int getActionCooldown() {
        return actionCooldown;
    }

    public int getDosesPerFetch() {
        return dosesPerFetch;
    }

    public int getBucketsPerFetch() {
        return bucketsPerFetch;
    }

    public int getMilkRegenTicks() {
        return milkRegenTicks;
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
            brain.eraseMemory(CftMemoryModuleType.MUST_RANCH.get());
        } else if (state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_RANCH.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_RANCH.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    private boolean isAtStructure(XoonglinEntity xoonglin, BlockPos structurePos) {
        return structurePos.closerToCenterThan(xoonglin.position(), CftConfig.HOME_WORK_RADIUS.get());
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_RANCH.get());
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

        for (ItemQuantity item : feed) {
            ItemStack[] matches = item.ingredient().getItems();
            if (matches.length > 0) {
                entries.add(JobDisplayEntry.item("gui.cft.job_inputs",
                        BuiltInRegistries.ITEM.getKey(matches[0].getItem()), item.quantity()));
                break;
            }
        }

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.RANCHER_JOB.get();
    }
}
