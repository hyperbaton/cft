package com.hyperbaton.cft.client.gui;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.job.TradeOffer;
import com.hyperbaton.cft.menu.TradeConfigMenu;
import com.hyperbaton.cft.network.TradeBoxClickPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Owner-facing trade configuration screen: real vanilla slots (drawn automatically by
 * AbstractContainerScreen) for the player's inventory, plus a list of trade boxes —
 * rendered directly when they all fit, or through a TradeListScrollPanel once max_trades
 * exceeds MAX_VISIBLE_TRADES (mirrors XoonglinInfoScreen's own needs-list behavior).
 * Trade box clicks bypass vanilla slot logic entirely via TradeBoxClickPacket.
 */
public class TradeConfigScreen extends AbstractContainerScreen<TradeConfigMenu> {

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "textures/gui/trade_config_background.png");
    private static final int TEXTURE_SIZE = 48;
    private static final int TILE = 16;

    private static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "textures/gui/trade_config_slot.png");
    private static final int SLOT_TEXTURE_SIZE = 18;

    private static final int BOX_SIZE = 18;
    private static final int WANTED_BOX_X = 20;
    private static final int GIVEN_BOX_X = 140;

    // The scroll panel must stay narrower than the screen (like NeedsScrollPanel is on
    // XoonglinInfoScreen), so its own background fill and scrollbar don't paint over the
    // nine-slice border tiles, which are TILE (16px) thick.
    private static final int SCROLL_PANEL_MARGIN = TILE + 2;

    private TradeListScrollPanel scrollPanel;

    public TradeConfigScreen(TradeConfigMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = TradeConfigMenu.TRADES_AREA_TOP
                + Math.min(menu.getMaxTrades(), TradeConfigMenu.MAX_VISIBLE_TRADES) * TradeConfigMenu.TRADE_ROW_HEIGHT
                + 14 + 58 + 18 + 10;
        this.inventoryLabelY = menu.getPlayerInvY() - 10;
    }

    @Override
    protected void init() {
        super.init();

        List<TradeOffer> offers = this.menu.getTradeOffersView();
        if (offers.size() > TradeConfigMenu.MAX_VISIBLE_TRADES) {
            int panelHeight = TradeConfigMenu.MAX_VISIBLE_TRADES * TradeConfigMenu.TRADE_ROW_HEIGHT;
            int panelWidth = imageWidth - SCROLL_PANEL_MARGIN * 2;
            scrollPanel = new TradeListScrollPanel(
                    Minecraft.getInstance(), this.font, panelWidth, panelHeight,
                    topPos + TradeConfigMenu.TRADES_AREA_TOP, leftPos + SCROLL_PANEL_MARGIN,
                    offers, this::sendTradeBoxClick
            );
        } else {
            scrollPanel = null;
        }
    }

    private void sendTradeBoxClick(int index, boolean wantedSide) {
        PacketDistributor.sendToServer(new TradeBoxClickPacket(this.menu.getXoonglinId(), index, wantedSide));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderNineSliceBackground(graphics);
        renderPlayerInventorySlots(graphics);

        if (scrollPanel == null) {
            List<TradeOffer> offers = this.menu.getTradeOffersView();
            for (int i = 0; i < offers.size(); i++) {
                int rowY = topPos + TradeConfigMenu.TRADES_AREA_TOP + i * TradeConfigMenu.TRADE_ROW_HEIGHT;
                drawBox(graphics, leftPos + WANTED_BOX_X, rowY);
                drawBox(graphics, leftPos + GIVEN_BOX_X, rowY);
            }
        }
    }

    private void drawBox(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + BOX_SIZE + 1, y + BOX_SIZE + 1, 0xFF8B8B8B);
        graphics.fill(x, y, x + BOX_SIZE, y + BOX_SIZE, 0xFF373737);
    }

    /**
     * Draws a recessed slot square under each of the 36 real player-inventory Slots.
     * Vanilla containers get this for free because their background PNG has the slot
     * squares baked in at fixed positions; ours can't bake them in because the trades
     * area (and therefore the inventory's Y offset) grows with max_trades, so the slot
     * art is stamped here instead, at each Slot's own (x, y) from TradeConfigMenu.
     */
    private void renderPlayerInventorySlots(GuiGraphics graphics) {
        for (Slot slot : this.menu.slots) {
            graphics.blit(SLOT_TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1,
                    0, 0, SLOT_TEXTURE_SIZE, SLOT_TEXTURE_SIZE, SLOT_TEXTURE_SIZE, SLOT_TEXTURE_SIZE);
        }
    }

    /**
     * Draws BACKGROUND_TEXTURE's 3x3 grid of 16px tiles (corners, edges, center) as a
     * classic Minecraft raised panel, tiled to fill any imageWidth/imageHeight rather
     * than stretched, so it stays crisp regardless of how many trade rows are shown.
     */
    private void renderNineSliceBackground(GuiGraphics graphics) {
        int innerWidth = imageWidth - 2 * TILE;
        int innerHeight = imageHeight - 2 * TILE;

        // Corners
        blitTile(graphics, leftPos, topPos, 0, 0, TILE, TILE);
        blitTile(graphics, leftPos + imageWidth - TILE, topPos, TILE * 2, 0, TILE, TILE);
        blitTile(graphics, leftPos, topPos + imageHeight - TILE, 0, TILE * 2, TILE, TILE);
        blitTile(graphics, leftPos + imageWidth - TILE, topPos + imageHeight - TILE, TILE * 2, TILE * 2, TILE, TILE);

        // Top / bottom edges
        for (int dx = 0; dx < innerWidth; dx += TILE) {
            int w = Math.min(TILE, innerWidth - dx);
            blitTile(graphics, leftPos + TILE + dx, topPos, TILE, 0, w, TILE);
            blitTile(graphics, leftPos + TILE + dx, topPos + imageHeight - TILE, TILE, TILE * 2, w, TILE);
        }

        // Left / right edges
        for (int dy = 0; dy < innerHeight; dy += TILE) {
            int h = Math.min(TILE, innerHeight - dy);
            blitTile(graphics, leftPos, topPos + TILE + dy, 0, TILE, TILE, h);
            blitTile(graphics, leftPos + imageWidth - TILE, topPos + TILE + dy, TILE * 2, TILE, TILE, h);
        }

        // Center
        for (int dy = 0; dy < innerHeight; dy += TILE) {
            int h = Math.min(TILE, innerHeight - dy);
            for (int dx = 0; dx < innerWidth; dx += TILE) {
                int w = Math.min(TILE, innerWidth - dx);
                blitTile(graphics, leftPos + TILE + dx, topPos + TILE + dy, TILE, TILE, w, h);
            }
        }
    }

    private void blitTile(GuiGraphics graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(BACKGROUND_TEXTURE, x, y, u, v, w, h, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        if (scrollPanel != null) {
            scrollPanel.render(graphics, mouseX, mouseY, delta);
            if (scrollPanel.hasTooltip()) {
                graphics.renderTooltip(this.font, scrollPanel.getTooltipStack(),
                        scrollPanel.getTooltipX(), scrollPanel.getTooltipY());
            }
        } else {
            List<TradeOffer> offers = this.menu.getTradeOffersView();
            for (int i = 0; i < offers.size(); i++) {
                int rowY = topPos + TradeConfigMenu.TRADES_AREA_TOP + i * TradeConfigMenu.TRADE_ROW_HEIGHT;
                TradeOffer offer = offers.get(i);

                String arrow = "->";
                graphics.drawString(this.font, arrow, leftPos + WANTED_BOX_X + BOX_SIZE + 8, rowY + 5, 0xFFFFFF, false);

                renderBoxItem(graphics, offer.wanted(), leftPos + WANTED_BOX_X + 1, rowY + 1, mouseX, mouseY);
                renderBoxItem(graphics, offer.given(), leftPos + GIVEN_BOX_X + 1, rowY + 1, mouseX, mouseY);
            }
        }

        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderBoxItem(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (stack.isEmpty()) return;
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(this.font, stack, x, y);
        if (mouseX >= x && mouseX < x + BOX_SIZE && mouseY >= y && mouseY < y + BOX_SIZE) {
            graphics.renderTooltip(this.font, stack, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollPanel != null && scrollPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && scrollPanel == null) {
            List<TradeOffer> offers = this.menu.getTradeOffersView();
            for (int i = 0; i < offers.size(); i++) {
                int rowY = topPos + TradeConfigMenu.TRADES_AREA_TOP + i * TradeConfigMenu.TRADE_ROW_HEIGHT;

                if (isInBox(mouseX, mouseY, leftPos + WANTED_BOX_X, rowY)) {
                    sendTradeBoxClick(i, true);
                    return true;
                }
                if (isInBox(mouseX, mouseY, leftPos + GIVEN_BOX_X, rowY)) {
                    sendTradeBoxClick(i, false);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollPanel != null && scrollPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollPanel != null && scrollPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollPanel != null && scrollPanel.isMouseOver(mouseX, mouseY)
                && scrollPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isInBox(double mouseX, double mouseY, int boxX, int boxY) {
        return mouseX >= boxX && mouseX < boxX + BOX_SIZE && mouseY >= boxY && mouseY < boxY + BOX_SIZE;
    }
}
