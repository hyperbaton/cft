package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.commons.lang3.mutable.MutableInt;

public class CftPacketHandler {

    private static final MutableInt ID = new MutableInt(0);
    private static final String VERSION = ModList.get().getModFileById(CftMod.MOD_ID).versionString();
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(CftMod.MOD_ID, "network"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void send(PacketDistributor.PacketTarget target, Object message)
    {
        CHANNEL.send(target, message);
    }
    public static void init() {
        CHANNEL.registerMessage(ID.incrementAndGet(), CheckOnXoonglinPacket.class, CheckOnXoonglinPacket::encode, CheckOnXoonglinPacket::new, CheckOnXoonglinPacket::handle);
    }
}
