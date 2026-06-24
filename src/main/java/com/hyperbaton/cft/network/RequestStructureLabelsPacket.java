package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.world.StructuresData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record RequestStructureLabelsPacket() implements CustomPacketPayload {

    public static final Type<RequestStructureLabelsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "request_structure_labels"));

    public static final StreamCodec<ByteBuf, RequestStructureLabelsPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RequestStructureLabelsPacket decode(ByteBuf buf) {
            return new RequestStructureLabelsPacket();
        }

        @Override
        public void encode(ByteBuf buf, RequestStructureLabelsPacket packet) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestStructureLabelsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");

                List<StructureLabelsPacket.Entry> entries = new ArrayList<>();
                for (Structure structure : data.getStructures()) {
                    if (!structure.getLeaderId().equals(player.getUUID())) continue;

                    String translatedName = net.minecraft.network.chat.Component.translatable(structure.getStructureTypeId()).getString();
                    String label = translatedName
                            + " (" + structure.getUserIds().size() + "/" + structure.getMaxUsers() + ")";

                    entries.add(new StructureLabelsPacket.Entry(structure.getKeyBlockPos(), label));
                }

                PacketDistributor.sendToPlayer(player, new StructureLabelsPacket(entries));
            }
        });
    }
}
