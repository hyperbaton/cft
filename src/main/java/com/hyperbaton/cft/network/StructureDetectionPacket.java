package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.network.client.StructureDetectionPacketClient;
import com.hyperbaton.cft.structure.StructureDetectionReason;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.List;

public record StructureDetectionPacket(
        boolean structureDetected,
        Optional<ResourceLocation> structureTypeId,
        StructureDetectionReason detectionReason,
        List<Component> validationDetails
) implements CustomPacketPayload {

    public static final Type<StructureDetectionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "structure_detection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureDetectionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StructureDetectionPacket::structureDetected,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), StructureDetectionPacket::structureTypeId,
            StructureDetectionReason.STREAM_CODEC,
            StructureDetectionPacket::detectionReason,
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()), StructureDetectionPacket::validationDetails,
            StructureDetectionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StructureDetectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> StructureDetectionPacketClient.handleClient(packet));
    }
}
