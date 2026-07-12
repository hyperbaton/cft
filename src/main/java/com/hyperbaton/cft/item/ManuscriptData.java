package com.hyperbaton.cft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * Attached to a written book to say which RostersData entry it belongs to, and who led
 * the settlement that produced it. Kept redundant with RostersData (rather than the sole
 * source of truth) so a physical book still remembers its origin even if its roster entry
 * is ever evicted.
 */
public record ManuscriptData(int rosterId, UUID leaderId) {

    public static final Codec<ManuscriptData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("roster_id").forGetter(ManuscriptData::rosterId),
            UUIDUtil.CODEC.fieldOf("leader_id").forGetter(ManuscriptData::leaderId)
    ).apply(inst, ManuscriptData::new));

    public static final StreamCodec<ByteBuf, ManuscriptData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ManuscriptData::rosterId,
            UUIDUtil.STREAM_CODEC, ManuscriptData::leaderId,
            ManuscriptData::new);
}
