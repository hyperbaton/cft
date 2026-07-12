package com.hyperbaton.cft.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The world's persisted roster of books produced by writers, shared across all of a
 * leader's Xoonglins regardless of whether any physical copy currently exists anywhere.
 */
public class RostersData extends SavedData {
    private static final String TAG_ENTRIES = "entries";

    private List<BookEntry> entries = new ArrayList<>();

    public static RostersData load(CompoundTag compoundTag, HolderLookup.Provider registries) {
        RostersData data = new RostersData();
        data.entries = new ArrayList<>();
        ListTag entryTags = compoundTag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
        for (Tag entryTag : entryTags) {
            data.entries.add(BookEntry.fromTag((CompoundTag) entryTag));
        }
        return data;
    }

    public static SavedData.Factory<RostersData> factory() {
        return new SavedData.Factory<>(RostersData::new, RostersData::load);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider registries) {
        ListTag entryTags = new ListTag();
        for (BookEntry entry : entries) {
            entryTags.add(entry.toTag());
        }
        compoundTag.put(TAG_ENTRIES, entryTags);
        return compoundTag;
    }

    public List<BookEntry> getEntries() {
        return entries;
    }

    public List<BookEntry> findByLeader(UUID leaderId) {
        return entries.stream().filter(e -> e.leaderId().equals(leaderId)).toList();
    }

    public int countByLeader(UUID leaderId) {
        return (int) entries.stream().filter(e -> e.leaderId().equals(leaderId)).count();
    }

    /** Adds a new entry, assigning it the next free id, and returns it. */
    public BookEntry add(UUID leaderId, String title, String authorName, List<String> pages) {
        int nextId = entries.stream().mapToInt(BookEntry::id).max().orElse(-1) + 1;
        BookEntry entry = new BookEntry(nextId, leaderId, title, authorName, pages);
        entries.add(entry);
        setDirty();
        return entry;
    }
}
