package com.hyperbaton.cft.network.client;

import com.hyperbaton.cft.client.gui.XoonglinInfoScreen;
import com.hyperbaton.cft.network.CheckOnXoonglinPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class CheckOnXoonglinPacketClient {

    public static void handleClient(CheckOnXoonglinPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().setScreen(new XoonglinInfoScreen(packet.getName(), packet));
            });
        }
    }
}