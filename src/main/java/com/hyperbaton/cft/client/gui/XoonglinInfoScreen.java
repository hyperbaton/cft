package com.hyperbaton.cft.client.gui;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.network.CheckOnXoonglinPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class XoonglinInfoScreen extends Screen {

    private static final ResourceLocation TEXTURE = 
        new ResourceLocation(CftMod.MOD_ID, "textures/gui/background_menu_1.png");
    public static final int MARGIN_PIXELS = 10;
    private final int imageWidth = 176, imageHeight = 176;
    private final CheckOnXoonglinPacket packet;

    public XoonglinInfoScreen(Component title, CheckOnXoonglinPacket packet) {
        super(title);
        this.minecraft = Minecraft.getInstance();
        this.packet = packet;
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (this.minecraft == null) {
            this.minecraft = Minecraft.getInstance();
        }
        
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        
        this.renderBackground(graphics);
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 176, 176);

        // Render title (Xoonglin's name)
        Component titleText = Component.literal(this.title.getString()).withStyle(ChatFormatting.BOLD);

        int titleWidth = this.font.width(titleText);
        int titleX = x + (imageWidth / 2) - (titleWidth / 2);

        graphics.drawString(this.font, titleText, titleX, y + MARGIN_PIXELS, 0x4040B0, false);

        // Render social class
        String socialClass = Component.translatable(packet.getSocialClass()).getString();
        graphics.drawString(this.font, socialClass, x + MARGIN_PIXELS, y + 30, 0x404040, false);
        
        // Render happiness
        String happinessLabel = Component.translatable("gui.cft.happiness").getString();
        String happinessValue = String.format("%.2f", packet.getHappiness());

        graphics.drawString(this.font, happinessLabel, x + MARGIN_PIXELS, y + 45, 0x404040, false);

        int happinessLabelWidth = this.font.width(happinessValue);
        int happinessLabelRightX = x + imageWidth - MARGIN_PIXELS - happinessLabelWidth;

        graphics.drawString(this.font, happinessValue, happinessLabelRightX, y + 45, 0x404040, false);

        // Render needs
        int barY = y + 65;
        int barWidth = 50;
        int barHeight = 8;

        for (Map.Entry<String, Double> need : packet.getNeedsSatisfaction().entrySet()) {
            String needLabel = Component.translatable(need.getKey()).getString();

            // Need name
            graphics.drawString(this.font, needLabel, x + MARGIN_PIXELS, barY, 0x404040, false);

            // Need bar
            int barX = x + imageWidth - MARGIN_PIXELS - barWidth;

            // Bar's background
            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF555555);

            // Bar's foreground
            int filledWidth = (int)(barWidth * need.getValue());
            graphics.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xFF00FF00);

            // Space between needs
            barY += 15;
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}