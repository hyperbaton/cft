package com.hyperbaton.cft.network.client;

import com.hyperbaton.cft.job.TradeOffer;
import com.hyperbaton.cft.menu.TradeConfigMenu;
import com.hyperbaton.cft.network.TradeBoxUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class TradeBoxUpdatePacketClient {

    public static void handleClient(TradeBoxUpdatePacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        Minecraft.getInstance().execute(() -> {
            if (player.containerMenu instanceof TradeConfigMenu menu) {
                menu.setTradeOffer(packet.tradeIndex(), new TradeOffer(packet.wanted(), packet.given()));
            }
        });
    }
}
