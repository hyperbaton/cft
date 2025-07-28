package com.hyperbaton.cft.client.gui;

import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ScrollPanel;

import java.util.Map;

class NeedsScrollPanel extends ScrollPanel {
    private static final int SIDE_MARGIN = 5;
    
    private final Map<String, Double> needs;
    private final int elementHeight = 15;
    private final Font font;

    public NeedsScrollPanel(Minecraft minecraft, Font font, int width, int height, int top, int left,
                            Map<String, Double> needs) {
        super(minecraft, width, height, top, left);
        this.needs = needs;
        this.font = font;
    }

    @Override
    protected int getContentHeight() {
        return needs.size() * elementHeight;
    }

    @Override
    protected void drawPanel(GuiGraphics graphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
        graphics.fill(left, top, left + width, top + height, 0xFF999999);

        int currentY = 0;
        int barWidth = 50;
        int barHeight = 8;

        for (Map.Entry<String, Double> need : needs.entrySet()) {
            if (currentY + elementHeight >= scrollDistance && currentY <= scrollDistance + height) {
                int adjustedY = top + currentY - (int) scrollDistance + SIDE_MARGIN;

                // Render label
                String needLabel = Component.translatable(need.getKey()).getString();
                graphics.drawString(this.font, needLabel, left + SIDE_MARGIN, adjustedY, 0x404040, false);

                // Render bar
                int barX = left + width - barWidth - 2 * SIDE_MARGIN;
                NeedsBarRenderer.renderBar(graphics, barX, adjustedY, barWidth, barHeight, need.getValue());
            }
            currentY += elementHeight;
        }
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.FOCUSED;
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }
}