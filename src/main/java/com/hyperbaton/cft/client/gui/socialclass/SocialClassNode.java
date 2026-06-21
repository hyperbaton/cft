package com.hyperbaton.cft.client.gui.socialclass;

import com.hyperbaton.cft.socialclass.SocialClass;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public record SocialClassNode(
        SocialClass socialClass,
        int x,
        int y,
        int width,
        int height,
        String displayName
) {

    private static final int BG_COLOR = 0xFF2D2D44;
    private static final int BG_HOVER = 0xFF3D3D5A;
    private static final int BG_SELECTED = 0xFF4A3D6A;
    private static final int BORDER_COLOR = 0xFF6A6A8A;
    private static final int BORDER_SELECTED = 0xFFAA88DD;
    private static final int TEXT_COLOR = 0xFFE0E0E0;

    public void render(GuiGraphics graphics, Font font, int offsetX, int offsetY, boolean hovered, boolean selected) {
        int rx = x + offsetX;
        int ry = y + offsetY;

        int bg = selected ? BG_SELECTED : (hovered ? BG_HOVER : BG_COLOR);
        int border = selected ? BORDER_SELECTED : BORDER_COLOR;

        graphics.fill(rx - 1, ry - 1, rx + width + 1, ry + height + 1, border);
        graphics.fill(rx, ry, rx + width, ry + height, bg);

        int textX = rx + (width - font.width(displayName)) / 2;
        int textY = ry + (height - font.lineHeight) / 2 + 1;
        graphics.drawString(font, displayName, textX, textY, TEXT_COLOR, false);
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }
}
