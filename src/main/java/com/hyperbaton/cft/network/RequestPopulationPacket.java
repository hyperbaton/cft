package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.socialclass.SocialStructureHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record RequestPopulationPacket() implements CustomPacketPayload {

    public static final Type<RequestPopulationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "request_population"));

    public static final StreamCodec<ByteBuf, RequestPopulationPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RequestPopulationPacket decode(ByteBuf buf) {
            return new RequestPopulationPacket();
        }

        @Override
        public void encode(ByteBuf buf, RequestPopulationPacket packet) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestPopulationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                Map<SocialClass, Integer> structure = SocialStructureHelper.computeSocialStructureForPlayer(level, player);
                Map<String, Integer> population = new HashMap<>();
                for (Map.Entry<SocialClass, Integer> entry : structure.entrySet()) {
                    population.put(entry.getKey().getId(), entry.getValue());
                }
                PacketDistributor.sendToPlayer(player, new PopulationUpdatePacket(population));
            }
        });
    }
}
