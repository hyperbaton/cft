package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.network.client.StructureDetectionPacketClient;
import com.hyperbaton.cft.structure.StructureDetectionReasons;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record StructureDetectionPacket(
        boolean structureDetected,
        String structureTypeId,
        StructureDetectionReasons detectionReason,
        List<String> validationDetails
) implements CustomPacketPayload {

    public static final Type<StructureDetectionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "structure_detection"));

    public static final StreamCodec<ByteBuf, StructureDetectionPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public StructureDetectionPacket decode(ByteBuf buf) {
            boolean detected = ByteBufCodecs.BOOL.decode(buf);
            String typeId = ByteBufCodecs.STRING_UTF8.decode(buf);
            StructureDetectionReasons reason = StructureDetectionReasons.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            List<String> details = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                details.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            return new StructureDetectionPacket(detected, typeId, reason, details);
        }

        @Override
        public void encode(ByteBuf buf, StructureDetectionPacket packet) {
            ByteBufCodecs.BOOL.encode(buf, packet.structureDetected);
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.structureTypeId);
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.detectionReason.name());
            ByteBufCodecs.VAR_INT.encode(buf, packet.validationDetails.size());
            for (String detail : packet.validationDetails) {
                ByteBufCodecs.STRING_UTF8.encode(buf, detail);
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StructureDetectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> StructureDetectionPacketClient.handleClient(packet));
    }
}
