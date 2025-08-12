package com.hyperbaton.cft.client.gui;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.network.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class XoonglinInfoScreen extends Screen {

    private static final ResourceLocation TEXTURE = 
        new ResourceLocation(CftMod.MOD_ID, "textures/gui/check_on_xoonglin_background.png");
    private static final int MARGIN_PIXELS = 10;
    private static final int MAX_VISIBLE_NEEDS = 7;
    private static final int UPDATE_FREQUENCY = 20; // Update every second

    private final int imageWidth = 176, imageHeight = 176;
    private CheckOnXoonglinPacket packet;
    private int ticksUntilNextUpdate = UPDATE_FREQUENCY;

    private NeedsScrollPanel needsScrollPanel;

    public XoonglinInfoScreen(Component title, CheckOnXoonglinPacket packet) {
        super(title);
        this.minecraft = Minecraft.getInstance();
        this.packet = packet;
    }

    @Override
    public void init() {
        super.init();

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (packet.getNeedsData().size() > MAX_VISIBLE_NEEDS) {
            int scrollPanelHeight = 15 * MAX_VISIBLE_NEEDS;
            needsScrollPanel = new NeedsScrollPanel(
                    minecraft,
                    this.font,
                    imageWidth - (MARGIN_PIXELS * 2),
                    scrollPanelHeight,
                    y + 65,
                    x + MARGIN_PIXELS,
                    packet.getNeedsData()
            );
        }
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

        // Render social class and job on the same line
        renderSocialClassAndJob(graphics, x, y + 30);
        
        // Render happiness
        String happinessLabel = Component.translatable("gui.cft.happiness").getString();
        String happinessValue = String.format("%.2f", packet.getHappiness());

        graphics.drawString(this.font, happinessLabel, x + MARGIN_PIXELS, y + 45, 0x404040, false);

        int happinessLabelWidth = this.font.width(happinessValue);
        int happinessLabelRightX = x + imageWidth - MARGIN_PIXELS - happinessLabelWidth;

        graphics.drawString(this.font, happinessValue, happinessLabelRightX, y + 45, 0x404040, false);

        // Render needs
        if (packet.getNeedsData().size() <= MAX_VISIBLE_NEEDS) {
            renderNeedsNormally(graphics, x, y, mouseX, mouseY);
        } else {
            needsScrollPanel.render(graphics, mouseX, mouseY, delta);

            // Render tooltip after rendering the panel, so it's on top
            Component tooltip = needsScrollPanel.getCurrentTooltip();
            if (tooltip != null) {
                graphics.renderTooltip(this.font, tooltip,
                        needsScrollPanel.getTooltipX(),
                        needsScrollPanel.getTooltipY());
            }

        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderSocialClassAndJob(GuiGraphics graphics, int x, int y) {
        // Render social class on the left
        String socialClass = Component.translatable(packet.getSocialClass()).getString();
        graphics.drawString(this.font, socialClass, x + MARGIN_PIXELS, y, 0x404040, false);
        
        // Render job on the right side (if present)
        if (packet.getJobId() != null) {
            String jobTranslationKey = "job." + packet.getJobId().getNamespace() + "." + packet.getJobId().getPath();
            String jobName = Component.translatable(jobTranslationKey).getString();
            
            int jobNameWidth = this.font.width(jobName);
            int jobNameRightX = x + imageWidth - MARGIN_PIXELS - jobNameWidth;
            
            // Use a different color for the job to distinguish it from social class
            graphics.drawString(this.font, jobName, jobNameRightX, y, 0x206020, false); // Green-ish color for job
        }
    }

    private void renderNeedsNormally(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int barY = y + 65;
        int barWidth = 50;
        int barHeight = 8;

        for (Map.Entry<String, NeedSatisfactionData> need : packet.getNeedsData().entrySet()) {
            String needLabel = Component.translatable(need.getKey()).getString();
            graphics.drawString(this.font, needLabel, x + MARGIN_PIXELS, barY, 0x404040, false);

            int barX = x + imageWidth - MARGIN_PIXELS - barWidth;
            NeedSatisfactionData data = need.getValue();
            boolean isHovered = NeedsBarRenderer.isMouseOver(mouseX, mouseY, barX, barY, barWidth, barHeight);

            NeedsBarRenderer.renderBar(graphics, barX, barY, barWidth, barHeight,
                data.satisfaction, data.damageThreshold, data.satisfactionThreshold, isHovered);

            if (isHovered) {
                graphics.renderTooltip(this.font,
                        NeedsBarRenderer.getTooltip(data.satisfaction),
                        mouseX, mouseY);
            }

            barY += 15;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (needsScrollPanel != null) {
            return needsScrollPanel.mouseScrolled(mouseX, mouseY, delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (--ticksUntilNextUpdate <= 0) {
            // Ask the server for an update
            CftPacketHandler.sendToServer(new RequestXoonglinInfoUpdatePacket(packet.getXoonglinId()));
            ticksUntilNextUpdate = UPDATE_FREQUENCY;
        }
    }

    public void updateData(XoonglinInfoUpdatePacket updatePacket) {
        this.packet = new CheckOnXoonglinPacket(
                updatePacket.getName(),
                updatePacket.getSocialClass(),
                updatePacket.getJobId(),
                updatePacket.getHappiness(),
                updatePacket.getNeedsData(),
                updatePacket.getXoonglinId()
        );

        // Update scroll panel if it exists
        if (needsScrollPanel != null) {
            needsScrollPanel.updateData(packet.getNeedsData());
        }
    }

}