package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftDataComponents;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.item.ManuscriptData;
import com.hyperbaton.cft.network.JobDisplayEntry;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.util.BookTextGenerator;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.util.JobUtil;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.world.BookEntry;
import com.hyperbaton.cft.world.RostersData;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Periodically writes an original book ("manuscript"): title/author/pages generated
 * procedurally, registered in the world's RostersData, and deposited in the base
 * container. Modeled on HomeArtisanJob's multi-day cadence (a book takes several days
 * of accumulated work, not one), rather than a daily-repeatable craft.
 */
public class WriterJob extends Job {

    public static final Codec<WriterJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.INT.fieldOf("frequency_days").forGetter(j -> j.frequencyDays),
            Codec.STRING.optionalFieldOf("required_structure", "").forGetter(j -> j.requiredStructure),
            Codec.INT.optionalFieldOf("pages_per_book", 6).forGetter(j -> j.pagesPerBook),
            ItemQuantity.CODEC.optionalFieldOf("input").forGetter(j -> j.input),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds),
            Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Job::getMinHappiness),
            Codec.BOOL.optionalFieldOf("available_to_babies", false).forGetter(Job::isAvailableToBabies),
            Codec.BOOL.optionalFieldOf("available_to_adults", true).forGetter(Job::isAvailableToAdults)
    ).apply(inst, WriterJob::new));

    private final double hoursPerDay;
    private final int frequencyDays;
    private final String requiredStructure;
    private final int pagesPerBook;
    private final Optional<ItemQuantity> input;

    public WriterJob(double hoursPerDay, int frequencyDays, String requiredStructure, int pagesPerBook,
                     Optional<ItemQuantity> input, List<String> requiredNeeds, double minHappiness,
                     boolean availableToBabies, boolean availableToAdults) {
        super(requiredNeeds, minHappiness, availableToBabies, availableToAdults);
        this.hoursPerDay = hoursPerDay;
        this.frequencyDays = Math.max(1, frequencyDays);
        this.requiredStructure = requiredStructure;
        this.pagesPerBook = pagesPerBook;
        this.input = input;
    }

    @Override
    public String getRequiredStructureType() {
        return requiredStructure.isEmpty() ? null : requiredStructure;
    }

    public Optional<ItemQuantity> getInput() {
        return input;
    }

    /** True if no input is configured, or the Xoonglin already carries enough of it. */
    public boolean hasEnoughInput(XoonglinEntity xoonglin) {
        if (input.isEmpty()) return true;
        var inventory = xoonglin.getInventory();
        int found = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && input.get().ingredient().test(stack)) {
                found += stack.getCount();
                if (found >= input.get().quantity()) return true;
            }
        }
        return false;
    }

    @Override
    public void tick(XoonglinEntity xoonglin, JobState state) {
        Level level = xoonglin.level();
        if (level.isClientSide) return;

        long dayIndex = Math.floorDiv(level.getDayTime(), 24000L);
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);

        // Day rollover: if yesterday's quota was never credited, the streak breaks.
        if (state.lastDayIndex == Long.MIN_VALUE) {
            state.lastDayIndex = dayIndex;
            state.creditedToday = false;
        } else if (dayIndex != state.lastDayIndex) {
            if (!state.creditedToday) {
                state.consecutiveDaysWorked = 0;
            }
            state.workedTicksToday = 0;
            state.lastDayIndex = dayIndex;
            state.creditedToday = false;
        }

        BlockPos structurePos = getRequiredStructureType() != null
                ? xoonglin.getAssignedStructurePos(requiredStructure) : null;
        boolean hasBase = getRequiredStructureType() != null
                ? structurePos != null
                : (xoonglin.getHome() != null && xoonglin.getHome().getEntrance() != null);
        boolean atBase = getRequiredStructureType() != null
                ? (structurePos != null && structurePos.closerToCenterThan(xoonglin.position(), CftConfig.HOME_WORK_RADIUS.get()))
                : JobUtil.isAtHome(xoonglin, CftConfig.HOME_WORK_RADIUS.get());

        if (atBase && canWork(xoonglin) && hasEnoughInput(xoonglin)) {
            state.workedTicksToday++;
        }

        // Credit the streak the instant today's quota is met
        if (!state.creditedToday && state.workedTicksToday >= neededTicks) {
            state.creditedToday = true;
            state.consecutiveDaysWorked++;

            if (state.consecutiveDaysWorked >= frequencyDays) {
                if (tryWriteBook((ServerLevel) level, xoonglin)) {
                    state.consecutiveDaysWorked = 0;
                }
                // If the roster is full, the streak is left at/above the threshold so
                // the book is written as soon as room frees up, without losing progress.
            }
        }

        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        if (getRequiredStructureType() != null && structurePos == null) {
            brain.setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), requiredStructure);
            brain.eraseMemory(CftMemoryModuleType.MUST_WRITE.get());
        } else if (hasBase && state.workedTicksToday < neededTicks) {
            brain.setMemory(CftMemoryModuleType.MUST_WRITE.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_WRITE.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    /** Returns true if a book was written (and the streak should reset). */
    private boolean tryWriteBook(ServerLevel level, XoonglinEntity xoonglin) {
        var leaderId = xoonglin.getLeaderId();
        if (leaderId == null) return false;

        RostersData rosters = level.getDataStorage().computeIfAbsent(RostersData.factory(), "rostersData");
        if (rosters.countByLeader(leaderId) >= CftConfig.MAX_ROSTER_SIZE.get()) {
            return false;
        }

        // Should already be held, carried into the inventory ahead of time by
        // WriteBehavior's fetching phase; this is just a defensive re-check.
        if (!hasEnoughInput(xoonglin)) {
            return false;
        }

        String title = BookTextGenerator.generateTitle(xoonglin.getRandom());
        if (title.length() > 32) title = title.substring(0, 32);
        String authorName = xoonglin.getName().getString();
        List<String> pages = BookTextGenerator.generatePages(xoonglin.getRandom(), pagesPerBook);

        BookEntry entry = rosters.add(leaderId, title, authorName, pages);

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> bookPages = pages.stream()
                .map(p -> Filterable.passThrough((Component) Component.literal(p)))
                .toList();
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(title), authorName, 0, bookPages, true));
        book.set(CftDataComponents.MANUSCRIPT.get(), new ManuscriptData(entry.id(), leaderId));

        if (input.isPresent()) {
            consumeInputFromInventory(xoonglin);
        }

        List<Container> containers = baseContainers(level, xoonglin);
        ItemStack leftover = ContainerUtil.insertIntoContainers(containers, book);
        if (!leftover.isEmpty()) {
            BlockPos dropPos = getRequiredStructureType() != null
                    ? xoonglin.getAssignedStructurePos(requiredStructure)
                    : (xoonglin.getHome() != null ? xoonglin.getHome().getEntrance() : xoonglin.blockPosition());
            if (dropPos == null) dropPos = xoonglin.blockPosition();
            Containers.dropItemStack(level, dropPos.getX() + 0.5, dropPos.getY() + 1.0, dropPos.getZ() + 0.5, leftover);
        }
        return true;
    }

    private void consumeInputFromInventory(XoonglinEntity xoonglin) {
        var inventory = xoonglin.getInventory();
        int remaining = input.get().quantity();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && input.get().ingredient().test(stack)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
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
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_WRITE.get());
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
        entries.add(JobDisplayEntry.progress("gui.cft.job_streak", state.consecutiveDaysWorked, frequencyDays));

        input.ifPresent(item -> {
            ItemStack[] matches = item.ingredient().getItems();
            if (matches.length > 0) {
                entries.add(JobDisplayEntry.item("gui.cft.job_inputs",
                        BuiltInRegistries.ITEM.getKey(matches[0].getItem()), item.quantity()));
            }
        });

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.WRITER_JOB.get();
    }
}
