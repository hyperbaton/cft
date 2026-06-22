package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CftMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CftPacketHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                CheckOnXoonglinPacket.TYPE,
                CheckOnXoonglinPacket.STREAM_CODEC,
                CheckOnXoonglinPacket::handle
        );

        registrar.playToClient(
                HomeDetectionPacket.TYPE,
                HomeDetectionPacket.STREAM_CODEC,
                HomeDetectionPacket::handle
        );

        registrar.playToServer(
                RequestXoonglinInfoUpdatePacket.TYPE,
                RequestXoonglinInfoUpdatePacket.STREAM_CODEC,
                RequestXoonglinInfoUpdatePacket::handle
        );

        registrar.playToClient(
                XoonglinInfoUpdatePacket.TYPE,
                XoonglinInfoUpdatePacket.STREAM_CODEC,
                XoonglinInfoUpdatePacket::handle
        );

        registrar.playToServer(
                RequestPopulationPacket.TYPE,
                RequestPopulationPacket.STREAM_CODEC,
                RequestPopulationPacket::handle
        );

        registrar.playToClient(
                PopulationUpdatePacket.TYPE,
                PopulationUpdatePacket.STREAM_CODEC,
                PopulationUpdatePacket::handle
        );
    }

    public static void sendToServer(Object message) {
        if (message instanceof net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
            PacketDistributor.sendToServer(payload);
        }
    }
}
