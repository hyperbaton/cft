package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.network.client.HomeDetectionPacketClient;
import com.hyperbaton.cft.structure.home.HomeDetectionReasons;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record HomeDetectionPacket(
        boolean homeDetected,
        String homeNeed,
        HomeDetectionReasons detectionReason,
        List<String> validationDetails
) implements CustomPacketPayload {

    public static final Type<HomeDetectionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "home_detection"));

    public static final StreamCodec<ByteBuf, HomeDetectionPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public HomeDetectionPacket decode(ByteBuf buf) {
            boolean homeDetected = ByteBufCodecs.BOOL.decode(buf);
            String homeNeed = ByteBufCodecs.STRING_UTF8.decode(buf);
            HomeDetectionReasons reason = HomeDetectionReasons.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            List<String> details = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                details.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            return new HomeDetectionPacket(homeDetected, homeNeed, reason, details);
        }

        @Override
        public void encode(ByteBuf buf, HomeDetectionPacket packet) {
            ByteBufCodecs.BOOL.encode(buf, packet.homeDetected);
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.homeNeed);
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

    public static void handle(HomeDetectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> HomeDetectionPacketClient.handleClient(packet));
    }

    public boolean isHomeDetected() { return homeDetected; }
    public String getHomeNeed() { return homeNeed; }
    public HomeDetectionReasons getDetectionReason() { return detectionReason; }
    public List<String> getValidationDetails() { return validationDetails; }
}
