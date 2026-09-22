package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.JobDisplayEntry;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.util.JobUtil;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A player-facing shopkeeper: hoards items for its configured trades from its base
 * container at the start of each work day, lets other players trade with it directly
 * (see XoonglinEntity.mobInteract), and deposits unsold stock plus received payment back
 * at day's end. Trades are configured at runtime by the leader (see TradeConfigMenu), not
 * via datapack — this job only carries the daily cadence and structural fields.
 */
public class TraderJob extends Job {

    public static final Codec<TraderJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.STRING.optionalFieldOf("required_structure", "").forGetter(j -> j.requiredStructure),
            Codec.INT.optionalFieldOf("max_trades", 4).forGetter(j -> j.maxTrades),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness),
            Codec.BOOL.optionalFieldOf("available_to_babies", false).forGetter(Job::isAvailableToBabies),
            Codec.BOOL.optionalFieldOf("available_to_adults", true).forGetter(Job::isAvailableToAdults)
    ).apply(inst, TraderJob::new));

    private final double hoursPerDay;
    private final String requiredStructure;
    private final int maxTrades;

    public TraderJob(double hoursPerDay, String requiredStructure, int maxTrades, List<String> requiredNeeds,
                      double minHappiness, boolean availableToBabies, boolean availableToAdults) {
        super(requiredNeeds, minHappiness, availableToBabies, availableToAdults);
        this.hoursPerDay = hoursPerDay;
        this.requiredStructure = requiredStructure;
        this.maxTrades = Math.max(1, maxTrades);
    }

    @Override
    public String getRequiredStructureType() {
        return requiredStructure.isEmpty() ? null : requiredStructure;
    }

    public int getMaxTrades() {
        return maxTrades;
    }

    @Override
    public void tick(XoonglinEntity xoonglin, JobState state) {
        Level level = xoonglin.level();
        if (level.isClientSide) return;

        long dayIndex = Math.floorDiv(level.getDayTime(), 24000L);
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);

        if (state.lastDayIndex == Long.MIN_VALUE) {
            state.lastDayIndex = dayIndex;
            state.creditedToday = false;
            state.dayStartHandled = false;
        } else if (dayIndex != state.lastDayIndex) {
            state.workedTicksToday = 0;
            state.lastDayIndex = dayIndex;
            state.creditedToday = false;
            state.dayStartHandled = false;
        }

        BlockPos structurePos = getRequiredStructureType() != null
                ? xoonglin.getAssignedStructurePos(requiredStructure) : null;
        boolean hasBase = getRequiredStructureType() != null
                ? structurePos != null
                : (xoonglin.getHome() != null && xoonglin.getHome().getEntrance() != null);
        boolean atBase = getRequiredStructureType() != null
                ? (structurePos != null && structurePos.closerToCenterThan(xoonglin.position(), CftConfig.HOME_WORK_RADIUS.get()))
                : JobUtil.isAtHome(xoonglin, CftConfig.HOME_WORK_RADIUS.get());

        if (atBase && canWork(xoonglin) && !state.dayStartHandled) {
            restock((ServerLevel) level, xoonglin);
            state.dayStartHandled = true;
        }

        if (atBase && canWork(xoonglin)) {
            state.workedTicksToday++;
        }

        if (!state.creditedToday && state.workedTicksToday >= neededTicks) {
            state.creditedToday = true;
            state.consecutiveDaysWorked++;
            deposit((ServerLevel) level, xoonglin);
        }

        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        if (getRequiredStructureType() != null && structurePos == null) {
            brain.setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), requiredStructure);
            brain.eraseMemory(CftMemoryModuleType.MUST_TRADE.get());
        } else if (hasBase && state.workedTicksToday < neededTicks) {
            brain.setMemory(CftMemoryModuleType.MUST_TRADE.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_TRADE.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    /** Hoards as much as possible of each active trade's "given" item from the base container. */
    private void restock(ServerLevel level, XoonglinEntity trader) {
        List<Container> containers = baseContainers(level, trader);
        if (containers.isEmpty()) return;
        for (TradeOffer offer : trader.getTradeOffers()) {
            if (!offer.isActive()) continue;
            hoardItem(containers, offer.given(), trader.getInventory());
        }
        containers.forEach(Container::setChanged);
    }

    /** Pulls as many matching stacks as fit into destination's remaining capacity for that item. */
    private void hoardItem(List<Container> sources, ItemStack template, SimpleContainer destination) {
        int capacity = remainingCapacityFor(destination, template);
        if (capacity <= 0) return;

        for (Container source : sources) {
            for (int i = 0; i < source.getContainerSize() && capacity > 0; i++) {
                ItemStack stack = source.getItem(i);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, template)) continue;
                int take = Math.min(capacity, stack.getCount());
                ItemStack taken = stack.copyWithCount(take);
                ItemStack leftover = destination.addItem(taken);
                int actuallyTaken = take - leftover.getCount();
                stack.shrink(actuallyTaken);
                capacity -= actuallyTaken;
            }
        }
    }

    private int remainingCapacityFor(SimpleContainer destination, ItemStack template) {
        int capacity = 0;
        int maxStack = template.getMaxStackSize();
        for (int i = 0; i < destination.getContainerSize(); i++) {
            ItemStack existing = destination.getItem(i);
            if (existing.isEmpty()) {
                capacity += maxStack;
            } else if (ItemStack.isSameItemSameComponents(existing, template)) {
                capacity += maxStack - existing.getCount();
            }
        }
        return capacity;
    }

    /** Returns everything the trader is carrying to the base container; keeps whatever doesn't fit. */
    private void deposit(ServerLevel level, XoonglinEntity trader) {
        List<Container> containers = baseContainers(level, trader);
        if (containers.isEmpty()) return;
        SimpleContainer inventory = trader.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            ItemStack leftover = ContainerUtil.insertIntoContainers(containers, stack);
            inventory.setItem(i, leftover);
        }
        containers.forEach(Container::setChanged);
    }

    public List<Container> baseContainers(ServerLevel level, XoonglinEntity xoonglin) {
        if (getRequiredStructureType() != null) {
            BlockPos structurePos = xoonglin.getAssignedStructurePos(requiredStructure);
            if (structurePos == null) return List.of();
            StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
            Structure structure = data.getStructures().stream()
                    .filter(s -> s.getKeyBlockPos().equals(structurePos))
                    .filter(s -> s.getStructureTypeId().equals(requiredStructure))
                    .findFirst().orElse(null);
            if (structure == null) return List.of();
            return ContainerUtil.findContainers(level, structure);
        }
        List<Container> containers = new ArrayList<>();
        if (xoonglin.getHome() != null) {
            for (BlockPos pos : xoonglin.getHome().getInteriorBlocks()) {
                if (level.getBlockEntity(pos) instanceof Container container) {
                    containers.add(container);
                }
            }
        }
        return containers;
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_TRADE.get());
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
        boolean canDoWork = canWork(xoonglin);
        boolean doneForDay = state.workedTicksToday >= neededTicks;
        boolean needsStructure = getRequiredStructureType() != null
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

        long activeTrades = xoonglin.getTradeOffers().stream().filter(TradeOffer::isActive).count();
        entries.add(JobDisplayEntry.progress("gui.cft.job_trades", (int) activeTrades, maxTrades));

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.TRADER_JOB.get();
    }
}
