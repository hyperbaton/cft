package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.core.UUIDUtil;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record RequestXoonglinInfoUpdatePacket(UUID xoonglinId) implements CustomPacketPayload {

    public static final Type<RequestXoonglinInfoUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "request_xoonglin_info"));

    public static final StreamCodec<ByteBuf, RequestXoonglinInfoUpdatePacket> STREAM_CODEC =
            UUIDUtil.STREAM_CODEC.map(RequestXoonglinInfoUpdatePacket::new, RequestXoonglinInfoUpdatePacket::xoonglinId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestXoonglinInfoUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                Entity entity = level.getEntity(packet.xoonglinId);

                if (entity instanceof XoonglinEntity xoonglin) {
                    if (xoonglin.getLeaderId() != null &&
                            xoonglin.getLeaderId().equals(player.getUUID())) {

                        Map<String, NeedSatisfactionData> needsData = xoonglin.getNeeds().stream()
                                .filter(needSatisfier -> !needSatisfier.getNeed().isHidden())
                                .collect(Collectors.toMap(
                                        needSatisfier -> needSatisfier.getNeed().getId(),
                                        needSatisfier -> new NeedSatisfactionData(
                                                needSatisfier.getSatisfaction(),
                                                needSatisfier.getNeed().getDamageThreshold(),
                                                needSatisfier.getNeed().getSatisfactionThreshold()
                                        )
                                ));

                        XoonglinInfoUpdatePacket updatePacket = new XoonglinInfoUpdatePacket(
                                xoonglin.getCustomName(),
                                xoonglin.getSocialClass().getId(),
                                xoonglin.getJob(),
                                xoonglin.getHappiness(),
                                needsData,
                                xoonglin.getUUID()
                        );

                        PacketDistributor.sendToPlayer(player, updatePacket);
                    }
                }
            }
        });
    }
}
