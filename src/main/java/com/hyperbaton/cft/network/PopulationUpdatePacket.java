package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.client.gui.socialclass.SocialClassBrowserScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record PopulationUpdatePacket(Map<String, Integer> population) implements CustomPacketPayload {

    public static final Type<PopulationUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "population_update"));

    public static final StreamCodec<ByteBuf, PopulationUpdatePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PopulationUpdatePacket decode(ByteBuf buf) {
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            Map<String, Integer> population = new HashMap<>();
            for (int i = 0; i < size; i++) {
                String classId = ByteBufCodecs.STRING_UTF8.decode(buf);
                int count = ByteBufCodecs.VAR_INT.decode(buf);
                population.put(classId, count);
            }
            return new PopulationUpdatePacket(population);
        }

        @Override
        public void encode(ByteBuf buf, PopulationUpdatePacket packet) {
            ByteBufCodecs.VAR_INT.encode(buf, packet.population.size());
            for (Map.Entry<String, Integer> entry : packet.population.entrySet()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, entry.getKey());
                ByteBufCodecs.VAR_INT.encode(buf, entry.getValue());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PopulationUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof SocialClassBrowserScreen screen) {
                screen.updatePopulation(packet.population());
            }
        });
    }
}
