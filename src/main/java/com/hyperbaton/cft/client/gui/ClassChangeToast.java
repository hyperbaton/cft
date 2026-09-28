package com.hyperbaton.cft.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Announces Xoonglins changing social class. Changes towards the same class in the same
 * direction are merged into a single toast while it is on screen, so a wave of promotions
 * doesn't flood the toast queue.
 */
public class ClassChangeToast implements Toast {
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/recipe");
    private static final long DISPLAY_TIME = 5000L;
    private static final int TEXT_X = 30;
    private static final int UPGRADE_COLOR = 0xFF2E7D32;
    private static final int DOWNGRADE_COLOR = 0xFFB71C1C;

    private final Token token;
    private final List<String> names = new ArrayList<>();
    private long lastChanged;
    private boolean changed = true;

    private ClassChangeToast(Token token, String firstName) {
        this.token = token;
        this.names.add(firstName);
    }

    public static void addOrUpdate(ToastComponent toasts, String xoonglinName, String toClass, boolean upgrade) {
        Token token = new Token(toClass, upgrade);
        ClassChangeToast existing = toasts.getToast(ClassChangeToast.class, token);
        if (existing == null) {
            toasts.addToast(new ClassChangeToast(token, xoonglinName));
        } else {
            existing.names.add(xoonglinName);
            existing.changed = true;
        }
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long timeSinceLastVisible) {
        if (changed) {
            lastChanged = timeSinceLastVisible;
            changed = false;
        }

        Font font = toasts.getMinecraft().font;
        graphics.blitSprite(BACKGROUND_SPRITE, 0, 0, width(), height());

        int color = token.upgrade() ? UPGRADE_COLOR : DOWNGRADE_COLOR;
        graphics.pose().pushPose();
        graphics.pose().translate(8, 7, 0);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.drawString(font, token.upgrade() ? "▲" : "▼", 0, 0, color, false);
        graphics.pose().popPose();

        Component title = Component.translatable(token.upgrade() ? "toast.cft.class_upgrade" : "toast.cft.class_downgrade");
        graphics.drawString(font, title, TEXT_X, 7, color, false);

        Component className = Component.translatable(token.toClass());
        Component description = names.size() == 1
                ? Component.translatable("toast.cft.class_change.single", names.get(0), className)
                : Component.translatable("toast.cft.class_change.multiple", names.size(), className);
        String text = font.plainSubstrByWidth(description.getString(), width() - TEXT_X - 6);
        graphics.drawString(font, text, TEXT_X, 18, 0xFF000000, false);

        return timeSinceLastVisible - lastChanged >= DISPLAY_TIME * toasts.getNotificationDisplayTimeMultiplier()
                ? Visibility.HIDE
                : Visibility.SHOW;
    }

    @Override
    public Object getToken() {
        return token;
    }

    private record Token(String toClass, boolean upgrade) {
    }
}
