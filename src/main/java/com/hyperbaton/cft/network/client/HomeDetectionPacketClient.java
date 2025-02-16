package com.hyperbaton.cft.network.client;

import com.hyperbaton.cft.network.HomeDetectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class HomeDetectionPacketClient {
    public static void handleClient(HomeDetectionPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            if (!packet.getHomeNeed().isBlank()) {
                player.sendSystemMessage(Component.literal("Inspecting house of type ")
                        .append(Component.translatable(packet.getHomeNeed())));
            } else {
                player.sendSystemMessage(Component.literal("Looking for a home..."));
            }
            player.sendSystemMessage(Component.literal(packet.getDetectionReason().getMessage()));
        }
    }
}
