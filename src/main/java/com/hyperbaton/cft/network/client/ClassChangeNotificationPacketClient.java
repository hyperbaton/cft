package com.hyperbaton.cft.network.client;

import com.hyperbaton.cft.CftClientConfig;
import com.hyperbaton.cft.CftClientConfig.NotificationMode;
import com.hyperbaton.cft.client.gui.ClassChangeToast;
import com.hyperbaton.cft.network.ClassChangeNotificationPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ClassChangeNotificationPacketClient {

    public static void handleClient(ClassChangeNotificationPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        NotificationMode mode = CftClientConfig.CLASS_CHANGE_NOTIFICATIONS.get();
        if (mode == NotificationMode.TOAST || mode == NotificationMode.BOTH) {
            ClassChangeToast.addOrUpdate(minecraft.getToasts(), packet.xoonglinName(), packet.toClass(), packet.upgrade());
        }
        if (mode == NotificationMode.CHAT || mode == NotificationMode.BOTH) {
            String key = packet.upgrade() ? "chat.cft.class_upgrade" : "chat.cft.class_downgrade";
            minecraft.player.displayClientMessage(
                    Component.translatable(key, packet.xoonglinName(),
                                    Component.translatable(packet.fromClass()),
                                    Component.translatable(packet.toClass()))
                            .withStyle(packet.upgrade() ? ChatFormatting.GREEN : ChatFormatting.RED),
                    false);
        }
    }
}
