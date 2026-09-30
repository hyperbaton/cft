package com.hyperbaton.cft.network.client;

import com.hyperbaton.cft.network.StructureDetectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class StructureDetectionPacketClient {
    public static void handleClient(StructureDetectionPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            if (!packet.structureTypeId().isBlank()) {
                player.sendSystemMessage(Component.translatable("detection.cft.inspecting",
                        Component.translatable(packet.structureTypeId())));
            }
            player.sendSystemMessage(packet.detectionReason().getMessage());
            if (!packet.validationDetails().isEmpty()) {
                packet.validationDetails().forEach(player::sendSystemMessage);
            }
        }
    }
}
