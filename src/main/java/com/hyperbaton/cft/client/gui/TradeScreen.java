package com.hyperbaton.cft.client.gui;

import com.hyperbaton.cft.network.ExecuteTradePacket;
import com.hyperbaton.cft.network.OpenTradeScreenPacket;
import com.hyperbaton.cft.network.TradeEntryData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * Plain (non-menu) screen for a customer trading with a Trader xoonglin: a scrollable
 * list of rows (wanted icon -> given icon), greyed out and non-clickable when stock is
 * insufficient. Clicking an active row sends ExecuteTradePacket.
 */
public class TradeScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ICON_SIZE = 16;
    private static final int PANEL_WIDTH = 200;

    private final UUID xoonglinId;
    private List<TradeEntryData> trades;

    public TradeScreen(OpenTradeScreenPacket packet) {
        super(packet.name());
        this.xoonglinId = packet.xoonglinId();
        this.trades = packet.trades();
    }

    public UUID getXoonglinId() {
        return xoonglinId;
    }

    public void refresh(OpenTradeScreenPacket packet) {
        this.trades = packet.trades();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);

        int panelHeight = Math.max(ROW_HEIGHT, trades.size() * ROW_HEIGHT) + 30;
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - panelHeight) / 2;

        graphics.fill(x, y, x + PANEL_WIDTH, y + panelHeight, 0xC0101010);
        graphics.drawCenteredString(this.font, this.title, x + PANEL_WIDTH / 2, y + 8, 0xFFFFFF);

        if (trades.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.cft.no_trades"),
                    x + PANEL_WIDTH / 2, y + 24, 0xA0A0A0);
        }

        int rowY = y + 24;
        for (TradeEntryData trade : trades) {
            boolean canAfford = trade.availableCount() >= trade.givenCount();
            int bg = canAfford ? 0xFF3A3A3A : 0xFF262626;
            boolean hovered = canAfford && mouseX >= x + 4 && mouseX < x + PANEL_WIDTH - 4
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT - 2;
            if (hovered) bg = 0xFF4A4A4A;

            graphics.fill(x + 4, rowY, x + PANEL_WIDTH - 4, rowY + ROW_HEIGHT - 2, bg);

            ItemStack wanted = new ItemStack(BuiltInRegistries.ITEM.get(trade.wantedItem()), trade.wantedCount());
            ItemStack given = new ItemStack(BuiltInRegistries.ITEM.get(trade.givenItem()), trade.givenCount());

            graphics.renderItem(wanted, x + 8, rowY);
            graphics.renderItemDecorations(this.font, wanted, x + 8, rowY);

            String arrow = "->";
            graphics.drawString(this.font, arrow, x + 8 + ICON_SIZE + 6, rowY + 4, 0xFFFFFF, false);

            int givenX = x + 8 + ICON_SIZE + 6 + this.font.width(arrow) + 6;
            graphics.renderItem(given, givenX, rowY);
            graphics.renderItemDecorations(this.font, given, givenX, rowY);

            if (!canAfford) {
                String outOfStock = Component.translatable("gui.cft.out_of_stock").getString();
                int textX = x + PANEL_WIDTH - 8 - this.font.width(outOfStock);
                graphics.drawString(this.font, outOfStock, textX, rowY + 4, 0xDD4040, false);
            }

            rowY += ROW_HEIGHT;
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelHeight = Math.max(ROW_HEIGHT, trades.size() * ROW_HEIGHT) + 30;
            int x = (width - PANEL_WIDTH) / 2;
            int y = (height - panelHeight) / 2;
            int rowY = y + 24;

            for (int i = 0; i < trades.size(); i++) {
                TradeEntryData trade = trades.get(i);
                boolean canAfford = trade.availableCount() >= trade.givenCount();
                if (canAfford && mouseX >= x + 4 && mouseX < x + PANEL_WIDTH - 4
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT - 2) {
                    PacketDistributor.sendToServer(new ExecuteTradePacket(xoonglinId, i));
                    return true;
                }
                rowY += ROW_HEIGHT;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
