package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record ChangeXoonglinJobPacket(UUID xoonglinId, ResourceLocation newJobId) implements CustomPacketPayload {

    public static final Type<ChangeXoonglinJobPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "change_xoonglin_job"));

    public static final StreamCodec<ByteBuf, ChangeXoonglinJobPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ChangeXoonglinJobPacket decode(ByteBuf buf) {
            UUID xoonglinId = UUIDUtil.STREAM_CODEC.decode(buf);
            ResourceLocation jobId = ResourceLocation.STREAM_CODEC.decode(buf);
            return new ChangeXoonglinJobPacket(xoonglinId, jobId);
        }

        @Override
        public void encode(ByteBuf buf, ChangeXoonglinJobPacket packet) {
            UUIDUtil.STREAM_CODEC.encode(buf, packet.xoonglinId);
            ResourceLocation.STREAM_CODEC.encode(buf, packet.newJobId);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChangeXoonglinJobPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                if (level.getEntity(packet.xoonglinId) instanceof XoonglinEntity xoonglin) {
                    if (xoonglin.getLeaderId() == null || !xoonglin.getLeaderId().equals(player.getUUID())) return;
                    if (xoonglin.getSocialClass() == null) return;
                    if (!xoonglin.getSocialClass().getJobsForAge(xoonglin.isBaby()).contains(packet.newJobId)) return;
                    xoonglin.removeFromJobStructures();
                    xoonglin.setJob(packet.newJobId);
                    xoonglin.getJobState().reset();
                }
            }
        });
    }
}
