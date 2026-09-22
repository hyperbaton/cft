package com.hyperbaton.cft.network.client;

import com.hyperbaton.cft.client.gui.TradeScreen;
import com.hyperbaton.cft.network.OpenTradeScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;

public class OpenTradeScreenPacketClient {

    public static void handleClient(OpenTradeScreenPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        Minecraft.getInstance().execute(() -> {
            Screen current = Minecraft.getInstance().screen;
            if (current instanceof TradeScreen tradeScreen && tradeScreen.getXoonglinId().equals(packet.xoonglinId())) {
                tradeScreen.refresh(packet);
            } else {
                Minecraft.getInstance().setScreen(new TradeScreen(packet));
            }
        });
    }
}
