package com.hyperbaton.cft.network;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RequestXoonglinInfoUpdatePacket {
    private final UUID xoonglinId;

    public RequestXoonglinInfoUpdatePacket(FriendlyByteBuf buf) {
        this.xoonglinId = buf.readUUID();
    }

    public RequestXoonglinInfoUpdatePacket(UUID xoonglinId) {
        this.xoonglinId = xoonglinId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(xoonglinId);
    }

    public static void handle(RequestXoonglinInfoUpdatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                handle(packet, player);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handle(RequestXoonglinInfoUpdatePacket packet, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(packet.xoonglinId);

        if (entity instanceof XoonglinEntity xoonglin) {
            if (xoonglin.getLeaderId() != null &&
                    xoonglin.getLeaderId().equals(player.getUUID())) {

                XoonglinInfoUpdatePacket updatePacket = new XoonglinInfoUpdatePacket(
                        xoonglin.getCustomName(),
                        xoonglin.getSocialClass().getId(),
                        xoonglin.getHappiness(),
                        xoonglin.getNeeds().stream()
                                .filter(needSatisfier -> !needSatisfier.getNeed().isHidden())
                                .collect(Collectors.toMap(
                                        needSatisfier -> needSatisfier.getNeed().getId(),
                                        NeedSatisfier::getSatisfaction
                                )),
                        xoonglin.getUUID()
                );

                CftPacketHandler.send(PacketDistributor.PLAYER.with(() -> player), updatePacket);
            }
        }
    }
}
