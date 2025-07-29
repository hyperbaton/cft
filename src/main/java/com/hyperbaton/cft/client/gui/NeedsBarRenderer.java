package com.hyperbaton.cft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

class NeedsBarRenderer {
    private static final int BORDER_COLOR = 0xFF2F2F2F;
    private static final int BAR_EMPTY = 0xFF404040;
    private static final int COLOR_RED = 0xFFCC0000;
    private static final int COLOR_ORANGE = 0xFFCC8800;
    private static final int THRESHOLD_INDICATOR_COLOR = 0x60FFFFFF;

    public static boolean isMouseOver(int mouseX, int mouseY, int barX, int barY, int width, int height) {
        return mouseX >= barX && mouseX <= barX + width &&
                mouseY >= barY && mouseY <= barY + height;
    }

    public static void renderBar(GuiGraphics graphics, int barX, int barY, int barWidth, int barHeight,
                                 double value, double damageThreshold, double satisfactionThreshold,
                                 boolean showIndicators) {
        // Render a border
        graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, BORDER_COLOR);

        // Render background
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, BAR_EMPTY);

        int filledWidth = (int) (barWidth * value);

        // Render the bar with a gradient
        if (filledWidth > 0) {
            int color = getBarColor(value, damageThreshold, satisfactionThreshold);
            int shadowColor = getDarkerColor(color);

            for (int i = 0; i < filledWidth; i++) {
                float ratio = (float) i / filledWidth;
                int barColor = blendColors(color, shadowColor, ratio);
                graphics.fill(barX + i, barY, barX + i + 1, barY + barHeight, barColor);
            }

            // Add shine on top
            graphics.fill(barX, barY, barX + filledWidth, barY + 1, getLighterColor(color));
        }

        // Render threshold indicators
        if (showIndicators) {
            renderThresholdIndicator(graphics, barX, barY, barWidth, barHeight, damageThreshold);
            renderThresholdIndicator(graphics, barX, barY, barWidth, barHeight, satisfactionThreshold);
        }
    }

    public static Component getTooltip(double value) {
        return Component.literal(String.format(
                "Satisfaction: %d%%",
                (int) (value * 100)
        ));
    }

    private static int getBarColor(double value, double damageThreshold, double satisfactionThreshold) {
        if (value < damageThreshold) {
            return COLOR_RED;
        } else if (value < satisfactionThreshold) {
            return COLOR_ORANGE;
        } else {
            double progress = (value - satisfactionThreshold) / (1.0 - satisfactionThreshold);

            int r = (int) (255 * (1 - progress));
            int g = (int) (255 - ((255 - 221) * progress));
            int b = 0;

            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    private static void renderThresholdIndicator(GuiGraphics graphics, int x, int y, int width, int height, double threshold) {
        int indicatorX = x + (int) (width * threshold);

        // Draw a thin vertical line
        graphics.fill(indicatorX - 1, y, indicatorX, y + height, THRESHOLD_INDICATOR_COLOR);

        // Add small ticks on top and bottom
        graphics.fill(indicatorX - 2, y, indicatorX + 1, y + 1, THRESHOLD_INDICATOR_COLOR);
        graphics.fill(indicatorX - 2, y + height - 1, indicatorX + 1, y + height, THRESHOLD_INDICATOR_COLOR);
    }

    private static int getDarkerColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = Math.max(0, r - 40);
        g = Math.max(0, g - 40);
        b = Math.max(0, b - 40);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int getLighterColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = Math.min(255, r + 60);
        g = Math.min(255, g + 60);
        b = Math.min(255, b + 60);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int blendColors(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}