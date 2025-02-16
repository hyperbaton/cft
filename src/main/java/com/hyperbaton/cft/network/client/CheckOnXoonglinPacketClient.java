package com.hyperbaton.cft.network.client;

import com.hyperbaton.cft.network.CheckOnXoonglinPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public class CheckOnXoonglinPacketClient {

    public static void handleClient(CheckOnXoonglinPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(MutableComponent.create(packet.getName().getContents())
                    .withStyle(ChatFormatting.BOLD)
                    .append(Component.literal(" class: "))
                    .append(Component.translatable(packet.getSocialClass())));
            player.sendSystemMessage(MutableComponent.create(packet.getName().getContents())
                    .append(Component.literal(" happiness: " + String.format("%.2f", packet.getHappiness()))));
            player.sendSystemMessage(MutableComponent.create(packet.getName().getContents())
                    .append(Component.literal(" needs: ")));
            for (Map.Entry<String, Double> entry : packet.getNeedsSatisfaction().entrySet()) {
                player.sendSystemMessage(Component.translatable(entry.getKey())
                        .append(" satisfied at " + String.format("%.2f", entry.getValue() * 100) + "%"));
            }
        }
    }
}