package com.hyperbaton.cft.structure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Why a structure detection failed, or that it succeeded. CFT's own reasons are in
 * {@link StructureDetectionReasons}; addon structure types can create their own.
 *
 * @param id       names the reason; the player reads its lang entry,
 *                 {@code detection.<namespace>.reason.<path>}
 * @param progress how far detection got. When several structure types share a key block and all of
 *                 them fail, the player is shown the failure with the highest progress: the type
 *                 that came closest. {@link StructureDetectionReasons} lists the progress of each
 *                 stage of detection, to place a new reason among them.
 */
public record StructureDetectionReason(ResourceLocation id, int progress) {

    public static final StreamCodec<ByteBuf, StructureDetectionReason> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, StructureDetectionReason::id,
            ByteBufCodecs.VAR_INT, StructureDetectionReason::progress,
            StructureDetectionReason::new
    );

    /** The reason, for the player. */
    public Component getMessage() {
        return Component.translatable("detection." + id.getNamespace() + ".reason." + id.getPath());
    }

    /** Whether detection got further with this reason than with another one. */
    public boolean isFurtherThan(StructureDetectionReason other) {
        return progress > other.progress;
    }
}
