package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.client.gui.socialclass.SocialClassBrowserScreen;
import com.hyperbaton.cft.socialclass.CensusStats;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PopulationUpdatePacket(CensusStats stats) implements CustomPacketPayload {

    public static final Type<PopulationUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "population_update"));

    public static final StreamCodec<ByteBuf, PopulationUpdatePacket> STREAM_CODEC =
            CensusStats.STREAM_CODEC.map(PopulationUpdatePacket::new, PopulationUpdatePacket::stats);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PopulationUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof SocialClassBrowserScreen screen) {
                screen.updateCensusStats(packet.stats());
            }
        });
    }
}
