package com.hyperbaton.cft.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Daily population snapshots per leader, used by the census to show trends.
 * Only the last {@link #MAX_DAYS} days are kept.
 */
public class PopulationHistoryData extends SavedData {
    public static final String DATA_NAME = "populationHistoryData";
    public static final int MAX_DAYS = 30;

    private static final String TAG_LAST_RECORDED_DAY = "lastRecordedDay";
    private static final String TAG_LEADERS = "leaders";
    private static final String TAG_LEADER = "leader";
    private static final String TAG_SNAPSHOTS = "snapshots";

    private long lastRecordedDay = -1;
    private final Map<UUID, Deque<PopulationSnapshot>> history = new HashMap<>();

    public static SavedData.Factory<PopulationHistoryData> factory() {
        return new SavedData.Factory<>(PopulationHistoryData::new, PopulationHistoryData::load);
    }

    public static PopulationHistoryData load(CompoundTag tag, HolderLookup.Provider registries) {
        PopulationHistoryData data = new PopulationHistoryData();
        data.lastRecordedDay = tag.getLong(TAG_LAST_RECORDED_DAY);
        for (Tag leaderTag : tag.getList(TAG_LEADERS, Tag.TAG_COMPOUND)) {
            CompoundTag leaderCompound = (CompoundTag) leaderTag;
            Deque<PopulationSnapshot> snapshots = new ArrayDeque<>();
            for (Tag snapshotTag : leaderCompound.getList(TAG_SNAPSHOTS, Tag.TAG_COMPOUND)) {
                snapshots.addLast(PopulationSnapshot.fromTag((CompoundTag) snapshotTag));
            }
            data.history.put(leaderCompound.getUUID(TAG_LEADER), snapshots);
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        tag.putLong(TAG_LAST_RECORDED_DAY, lastRecordedDay);
        ListTag leaders = new ListTag();
        for (Map.Entry<UUID, Deque<PopulationSnapshot>> entry : history.entrySet()) {
            CompoundTag leaderCompound = new CompoundTag();
            leaderCompound.putUUID(TAG_LEADER, entry.getKey());
            ListTag snapshots = new ListTag();
            entry.getValue().forEach(snapshot -> snapshots.add(snapshot.toTag()));
            leaderCompound.put(TAG_SNAPSHOTS, snapshots);
            leaders.add(leaderCompound);
        }
        tag.put(TAG_LEADERS, leaders);
        return tag;
    }

    public long getLastRecordedDay() {
        return lastRecordedDay;
    }

    public void setLastRecordedDay(long day) {
        this.lastRecordedDay = day;
        setDirty();
    }

    public void record(UUID leaderId, PopulationSnapshot snapshot) {
        Deque<PopulationSnapshot> snapshots = history.computeIfAbsent(leaderId, id -> new ArrayDeque<>());
        snapshots.addLast(snapshot);
        while (snapshots.size() > MAX_DAYS) {
            snapshots.removeFirst();
        }
        setDirty();
    }

    /** Oldest first. */
    public List<PopulationSnapshot> getHistory(UUID leaderId) {
        return List.copyOf(history.getOrDefault(leaderId, new ArrayDeque<>()));
    }
}
