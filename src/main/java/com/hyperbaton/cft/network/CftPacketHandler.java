package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.commons.lang3.mutable.MutableInt;

public class CftPacketHandler {
    private static final MutableInt ID = new MutableInt(0);
    private static final String VERSION = ModList.get().getModFileById(CftMod.MOD_ID).versionString();
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CftMod.MOD_ID, "network"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals
    );

    public static void send(PacketDistributor.PacketTarget target, Object message) {
        CHANNEL.send(target, message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void init() {
        CHANNEL.registerMessage(ID.incrementAndGet(), 
            CheckOnXoonglinPacket.class, 
            CheckOnXoonglinPacket::encode, 
            CheckOnXoonglinPacket::new, 
            CheckOnXoonglinPacket::handle
        );
        
        CHANNEL.registerMessage(ID.incrementAndGet(), 
            HomeDetectionPacket.class, 
            HomeDetectionPacket::encode, 
            HomeDetectionPacket::new, 
            HomeDetectionPacket::handle
        );

        CHANNEL.messageBuilder(RequestXoonglinInfoUpdatePacket.class, ID.incrementAndGet(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(RequestXoonglinInfoUpdatePacket::encode)
            .decoder(RequestXoonglinInfoUpdatePacket::new)
            .consumerMainThread(RequestXoonglinInfoUpdatePacket::handle)
            .add();

        CHANNEL.messageBuilder(XoonglinInfoUpdatePacket.class, ID.incrementAndGet(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(XoonglinInfoUpdatePacket::encode)
            .decoder(XoonglinInfoUpdatePacket::new)
            .consumerMainThread(XoonglinInfoUpdatePacket::handle)
            .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}