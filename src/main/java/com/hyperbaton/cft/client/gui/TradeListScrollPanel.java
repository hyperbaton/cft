package com.hyperbaton.cft.client.gui;

import com.hyperbaton.cft.job.TradeOffer;
import com.hyperbaton.cft.menu.TradeConfigMenu;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.widget.ScrollPanel;

import java.util.List;

/**
 * Scrollable list of trade boxes for TradeConfigScreen, used once a trader's configured
 * max_trades exceeds the number of rows that fit on screen (mirrors NeedsScrollPanel's
 * role on the Info tab's needs list).
 */
class TradeListScrollPanel extends ScrollPanel {

    static final int BOX_SIZE = 18;
    // Relative to this panel's own (narrower-than-screen) left edge, not the screen's —
    // tuned to leave clearance for the vertical scrollbar ScrollPanel draws at its right edge.
    static final int WANTED_BOX_X = 14;
    static final int GIVEN_BOX_X = 104;

    interface RowClickHandler {
        void onClick(int index, boolean wantedSide);
    }

    private final Font font;
    private final List<TradeOffer> offers;
    private final RowClickHandler onRowClick;

    private ItemStack tooltipStack = ItemStack.EMPTY;
    private int tooltipX, tooltipY;

    TradeListScrollPanel(Minecraft minecraft, Font font, int width, int height, int top, int left,
                          List<TradeOffer> offers, RowClickHandler onRowClick) {
        super(minecraft, width, height, top, left);
        this.font = font;
        this.offers = offers;
        this.onRowClick = onRowClick;
    }

    @Override
    protected int getContentHeight() {
        return offers.size() * TradeConfigMenu.TRADE_ROW_HEIGHT;
    }

    @Override
    protected void drawPanel(GuiGraphics graphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
        // Covers ScrollPanel's own default (vanilla dirt-gradient) background so the
        // screen's own nine-slice panel shows through instead, matching NeedsScrollPanel.
        graphics.fill(left, top, left + width, top + height, 0xFFC6C6C6);

        int rowHeight = TradeConfigMenu.TRADE_ROW_HEIGHT;
        tooltipStack = ItemStack.EMPTY;

        for (int i = 0; i < offers.size(); i++) {
            int currentY = i * rowHeight;
            if (currentY + rowHeight < scrollDistance || currentY > scrollDistance + height) continue;

            int rowY = relativeY + currentY;
            TradeOffer offer = offers.get(i);

            drawBox(graphics, left + WANTED_BOX_X, rowY);
            drawBox(graphics, left + GIVEN_BOX_X, rowY);

            String arrow = "->";
            graphics.drawString(font, arrow, left + WANTED_BOX_X + BOX_SIZE + 8, rowY + 5, 0xFFFFFF, false);

            renderBoxItem(graphics, offer.wanted(), left + WANTED_BOX_X + 1, rowY + 1, mouseX, mouseY);
            renderBoxItem(graphics, offer.given(), left + GIVEN_BOX_X + 1, rowY + 1, mouseX, mouseY);
        }
    }

    private void drawBox(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + BOX_SIZE + 1, y + BOX_SIZE + 1, 0xFF8B8B8B);
        graphics.fill(x, y, x + BOX_SIZE, y + BOX_SIZE, 0xFF373737);
    }

    private void renderBoxItem(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (stack.isEmpty()) return;
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
        if (mouseX >= x && mouseX < x + BOX_SIZE && mouseY >= y && mouseY < y + BOX_SIZE
                && mouseY >= top && mouseY < bottom) {
            tooltipStack = stack;
            tooltipX = mouseX;
            tooltipY = mouseY;
        }
    }

    @Override
    protected boolean clickPanel(double relativeX, double relativeY, int button) {
        int rowHeight = TradeConfigMenu.TRADE_ROW_HEIGHT;
        int index = (int) relativeY / rowHeight;
        if (index < 0 || index >= offers.size()) return false;
        int localX = (int) relativeX;

        if (localX >= WANTED_BOX_X && localX < WANTED_BOX_X + BOX_SIZE) {
            onRowClick.onClick(index, true);
            return true;
        }
        if (localX >= GIVEN_BOX_X && localX < GIVEN_BOX_X + BOX_SIZE) {
            onRowClick.onClick(index, false);
            return true;
        }
        return false;
    }

    boolean hasTooltip() {
        return !tooltipStack.isEmpty();
    }

    ItemStack getTooltipStack() {
        return tooltipStack;
    }

    int getTooltipX() {
        return tooltipX;
    }

    int getTooltipY() {
        return tooltipY;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
    }
}
