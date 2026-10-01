package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.network.client.ClassChangeNotificationPacketClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Tells a leader that one of their Xoonglins has changed social class.
 */
public record ClassChangeNotificationPacket(
        String xoonglinName,
        ResourceLocation fromClass,
        ResourceLocation toClass,
        boolean upgrade
) implements CustomPacketPayload {

    public static final Type<ClassChangeNotificationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "class_change_notification"));

    public static final StreamCodec<ByteBuf, ClassChangeNotificationPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClassChangeNotificationPacket::xoonglinName,
            ResourceLocation.STREAM_CODEC, ClassChangeNotificationPacket::fromClass,
            ResourceLocation.STREAM_CODEC, ClassChangeNotificationPacket::toClass,
            ByteBufCodecs.BOOL, ClassChangeNotificationPacket::upgrade,
            ClassChangeNotificationPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClassChangeNotificationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClassChangeNotificationPacketClient.handleClient(packet));
    }
}
