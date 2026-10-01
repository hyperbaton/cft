package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.client.render.StructureLabelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record StructureLabelsPacket(List<Entry> entries) implements CustomPacketPayload {

    /** A label shown above a structure's key block. */
    public record Entry(BlockPos pos, Component label) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Entry::pos,
                ComponentSerialization.STREAM_CODEC, Entry::label,
                Entry::new
        );
    }

    public static final Type<StructureLabelsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "structure_labels"));

    // Labels are components, so each player reads them in their own language
    public static final StreamCodec<RegistryFriendlyByteBuf, StructureLabelsPacket> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(StructureLabelsPacket::new, StructureLabelsPacket::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StructureLabelsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> StructureLabelRenderer.updateLabels(packet.entries));
    }
}
