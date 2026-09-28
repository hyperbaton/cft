package com.hyperbaton.cft.world;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * A leader's population on a given in-game day.
 *
 * @param day            in-game day number (game time / 24000, unaffected by /time set)
 * @param classCounts    Xoonglins per social class id
 * @param averageHappiness average happiness over the whole population
 */
public record PopulationSnapshot(long day, Map<String, Integer> classCounts, double averageHappiness) {

    private static final String TAG_DAY = "day";
    private static final String TAG_COUNTS = "counts";
    private static final String TAG_AVERAGE_HAPPINESS = "averageHappiness";

    public static final StreamCodec<ByteBuf, PopulationSnapshot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, PopulationSnapshot::day,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), PopulationSnapshot::classCounts,
            ByteBufCodecs.DOUBLE, PopulationSnapshot::averageHappiness,
            PopulationSnapshot::new
    );

    public int total() {
        return classCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(TAG_DAY, day);
        CompoundTag counts = new CompoundTag();
        classCounts.forEach(counts::putInt);
        tag.put(TAG_COUNTS, counts);
        tag.putDouble(TAG_AVERAGE_HAPPINESS, averageHappiness);
        return tag;
    }

    public static PopulationSnapshot fromTag(CompoundTag tag) {
        Map<String, Integer> counts = new HashMap<>();
        CompoundTag countsTag = tag.getCompound(TAG_COUNTS);
        for (String classId : countsTag.getAllKeys()) {
            counts.put(classId, countsTag.getInt(classId));
        }
        return new PopulationSnapshot(tag.getLong(TAG_DAY), counts, tag.getDouble(TAG_AVERAGE_HAPPINESS));
    }
}
