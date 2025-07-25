package com.hyperbaton.cft.client.screen;

import com.hyperbaton.cft.client.menu.XoonglinSummaryMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;

import java.awt.*;

public class XoonglinSummaryScreen extends Screen {
    private final Component name;
    private final Component socialClass;
    private final double happiness;
    private final List<NeedEntry> needs;
    private final XoonglinSummaryMenu menu;

    public XoonglinSummaryScreen(XoonglinSummaryMenu menu, Component name, Component socialClass, double happiness, List<NeedEntry> needs) {
        super(name);
        this.menu = menu;
        this.name = name;
        this.socialClass = socialClass;
        this.happiness = happiness;
        this.needs = needs;
    }

    @Override
    protected void init() {
        // Initialize UI elements
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.name, this.width / 2, 15, 0xFFFFFF);
        drawString(poseStack, this.font, this.socialClass, 10, 35, 0xFFFFFF);
        drawString(poseStack, this.font, "Happiness: " + happiness, this.width - 100, 35, 0xFFFFFF);

        int y = 60;
        for (NeedEntry need : needs) {
            drawString(poseStack, this.font, need.getName(), 30, y, 0xFFFFFF);
            need.getIcon().render(poseStack, 10, y);
            need.getSatisfactionBar().render(poseStack, 100, y);
            y += 20;
        }

        super.render(poseStack, mouseX, mouseY, delta);
    }
}

