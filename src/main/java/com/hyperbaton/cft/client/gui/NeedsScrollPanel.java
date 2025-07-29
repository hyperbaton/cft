package com.hyperbaton.cft.client.gui;

import com.hyperbaton.cft.network.NeedSatisfactionData;
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
    
    private Map<String, NeedSatisfactionData> needsData;
    private final int elementHeight = 15;
    private final Font font;
    private Component currentTooltip = null;
    private int tooltipX, tooltipY;

    public NeedsScrollPanel(Minecraft minecraft, Font font, int width, int height, int top, int left,
                           Map<String, NeedSatisfactionData> needsData) {
        super(minecraft, width, height, top, left);
        this.needsData = needsData;
        this.font = font;
    }

    @Override
    protected int getContentHeight() {
        return needsData.size() * elementHeight;
    }

    @Override
    protected void drawPanel(GuiGraphics graphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
        graphics.fill(left, top, left + width, top + height, 0xFF999999);

        int currentY = 0;
        int barWidth = 50;
        int barHeight = 8;
        currentTooltip = null;

        for (Map.Entry<String, NeedSatisfactionData> need : needsData.entrySet()) {
            if (currentY + elementHeight >= scrollDistance && currentY <= scrollDistance + height) {
                int adjustedY = top + currentY - (int) scrollDistance + SIDE_MARGIN;

                String needLabel = Component.translatable(need.getKey()).getString();
                graphics.drawString(this.font, needLabel, left + SIDE_MARGIN, adjustedY, 0x404040, false);

                int barX = left + width - barWidth - 2 * SIDE_MARGIN;
                NeedSatisfactionData data = need.getValue();
                boolean isHovered = NeedsBarRenderer.isMouseOver(mouseX, mouseY, barX, adjustedY, barWidth, barHeight);
                NeedsBarRenderer.renderBar(graphics, barX, adjustedY, barWidth, barHeight, 
                    data.satisfaction, data.damageThreshold, data.satisfactionThreshold, isHovered);

                if (isHovered) {
                    currentTooltip = NeedsBarRenderer.getTooltip(data.satisfaction);
                    tooltipX = mouseX;
                    tooltipY = mouseY;
                }
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

    public Component getCurrentTooltip() {
        return currentTooltip;
    }

    public int getTooltipX() {
        return tooltipX;
    }

    public int getTooltipY() {
        return tooltipY;
    }

    public void updateData(Map<String, NeedSatisfactionData> newNeedsData) {
        this.needsData = newNeedsData;
    }

}