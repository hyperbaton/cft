package com.hyperbaton.cft.client.gui.socialclass;

import net.minecraft.client.gui.GuiGraphics;

public class ClassGraphRenderer {

    private static final int UPGRADE_COLOR = 0xFF44AA44;
    private static final int DOWNGRADE_COLOR = 0xFFAA4444;
    private static final int LINE_THICKNESS = 2;
    private static final int ARROW_SIZE = 4;
    private static final int LATERAL_SEPARATION = 6;
    private static final int VERTICAL_SEPARATION = 4;

    public static void renderEdge(GuiGraphics graphics, GraphEdge edge, int offsetX, int offsetY) {
        int color = edge.isUpgrade() ? UPGRADE_COLOR : DOWNGRADE_COLOR;

        int lateralShift = edge.isUpgrade() ? -LATERAL_SEPARATION : LATERAL_SEPARATION;
        int verticalShift = edge.isUpgrade() ? -VERTICAL_SEPARATION : VERTICAL_SEPARATION;

        int fromX = edge.from().centerX() + offsetX + lateralShift;
        int toX = edge.to().centerX() + offsetX + lateralShift;

        int fromY, toY;
        if (edge.isUpgrade()) {
            fromY = edge.from().y() + offsetY - 1;
            toY = edge.to().y() + edge.to().height() + offsetY + 1;
        } else {
            fromY = edge.from().y() + edge.from().height() + offsetY + 1;
            toY = edge.to().y() + offsetY - 2;
        }

        int midY = (fromY + toY) / 2 + verticalShift;

        drawVerticalLine(graphics, fromX, fromY, midY, color);
        drawHorizontalLine(graphics, fromX, toX, midY, color);
        drawVerticalLine(graphics, toX, midY, toY, color);

        // Fill elbow corners so there are no gaps
        fillCorner(graphics, fromX, midY, color);
        fillCorner(graphics, toX, midY, color);

        renderArrowhead(graphics, toX, toY, edge.isUpgrade(), color);
    }

    private static void drawVerticalLine(GuiGraphics graphics, int x, int y1, int y2, int color) {
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int left = x - LINE_THICKNESS / 2;
        graphics.fill(left, minY, left + LINE_THICKNESS, maxY, color);
    }

    private static void drawHorizontalLine(GuiGraphics graphics, int x1, int x2, int y, int color) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int top = y - LINE_THICKNESS / 2;
        graphics.fill(minX, top, maxX, top + LINE_THICKNESS, color);
    }

    private static void fillCorner(GuiGraphics graphics, int x, int y, int color) {
        int left = x - LINE_THICKNESS / 2;
        int top = y - LINE_THICKNESS / 2;
        graphics.fill(left, top, left + LINE_THICKNESS, top + LINE_THICKNESS, color);
    }

    private static void renderArrowhead(GuiGraphics graphics, int x, int y, boolean pointingUp, int color) {
        int left = x - LINE_THICKNESS / 2;
        int right = left + LINE_THICKNESS;
        if (pointingUp) {
            for (int i = 0; i < ARROW_SIZE; i++) {
                graphics.fill(left - i, y + i, right + i, y + i + 1, color);
            }
        } else {
            for (int i = 0; i < ARROW_SIZE; i++) {
                graphics.fill(left - i, y - i, right + i, y - i + 1, color);
            }
        }
    }
}
