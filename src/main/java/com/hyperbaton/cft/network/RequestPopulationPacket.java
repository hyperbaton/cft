package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.socialclass.CensusStats;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestPopulationPacket() implements CustomPacketPayload {

    public static final Type<RequestPopulationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "request_population"));

    public static final StreamCodec<ByteBuf, RequestPopulationPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestPopulationPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestPopulationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CensusStats stats = CensusStats.build(player.serverLevel(), player.getUUID());
                PacketDistributor.sendToPlayer(player, new PopulationUpdatePacket(stats));
            }
        });
    }
}
