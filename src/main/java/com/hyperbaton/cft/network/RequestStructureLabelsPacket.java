package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.type.HouseStructureType;
import com.hyperbaton.cft.world.StructuresData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

                    String label = buildLabel(structure, level);
                    entries.add(new StructureLabelsPacket.Entry(structure.getKeyBlockPos(), label));
                }

                PacketDistributor.sendToPlayer(player, new StructureLabelsPacket(entries));
            }
        });
    }

    private static String buildLabel(Structure structure, ServerLevel level) {
        String translatedName = Component.translatable(structure.getStructureTypeId()).getString();

        if (isOccupiedSingleUserHouse(structure)) {
            UUID userId = structure.getUserIds().get(0);
            Entity user = level.getEntity(userId);
            if (user instanceof XoonglinEntity xoonglin && xoonglin.getCustomName() != null) {
                return Component.translatable("gui.cft.home_label.occupied",
                        xoonglin.getCustomName().getString()).getString();
            }
        }

        return translatedName + " (" + structure.getUserIds().size() + "/" + structure.getMaxUsers() + ")";
    }

    private static boolean isOccupiedSingleUserHouse(Structure structure) {
        if (structure.getMaxUsers() != 1 || structure.getUserIds().isEmpty()) return false;
        if (CftRegistry.STRUCTURES == null) return false;
        StructureType type = CftRegistry.STRUCTURES.stream()
                .filter(st -> st.getId().equals(structure.getStructureTypeId()))
                .findFirst()
                .orElse(null);
        return type instanceof HouseStructureType;
    }
}
