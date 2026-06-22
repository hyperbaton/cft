package com.hyperbaton.cft.client.gui;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.network.*;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public class XoonglinInfoScreen extends Screen {

    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "textures/gui/check_on_xoonglin_background.png");
    private static final int MARGIN_PIXELS = 10;
    private static final int MAX_VISIBLE_NEEDS = 7;
    private static final int UPDATE_FREQUENCY = 20;
    private static final int ICON_ROTATE_TICKS = 40;
    private static final int ICON_SIZE = 16;

    private final int imageWidth = 220, imageHeight = 176;
    private CheckOnXoonglinPacket packet;
    private int ticksUntilNextUpdate = UPDATE_FREQUENCY;
    private int tickCounter = 0;

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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Skip default blur/darkening — we draw our own background texture
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (this.minecraft == null) {
            this.minecraft = Minecraft.getInstance();
        }
        
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 220, 176);

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
            NeedSatisfactionData data = need.getValue();
            int textX = x + MARGIN_PIXELS;

            if (!data.icons.isEmpty()) {
                ItemStack iconStack = getIconStack(data.icons);
                graphics.renderItem(iconStack, textX, barY - 4);
                textX += ICON_SIZE + 2;
            }

            String needLabel = Component.translatable(need.getKey()).getString();
            graphics.drawString(this.font, needLabel, textX, barY, 0x404040, false);

            int barX = x + imageWidth - MARGIN_PIXELS - barWidth;
            boolean isHovered = NeedsBarRenderer.isMouseOver(mouseX, mouseY, barX, barY, barWidth, barHeight);

            NeedsBarRenderer.renderBar(graphics, barX, barY, barWidth, barHeight,
                data.satisfaction, data.damageThreshold, data.satisfactionThreshold, isHovered);

            if (isHovered) {
                graphics.renderTooltip(this.font,
                        NeedsBarRenderer.getTooltip(data.satisfaction),
                        mouseX, mouseY);
            }

            barY += 18;
        }
    }

    private ItemStack getIconStack(List<ResourceLocation> icons) {
        int index = icons.size() > 1 ? (tickCounter / ICON_ROTATE_TICKS) % icons.size() : 0;
        return new ItemStack(BuiltInRegistries.ITEM.get(icons.get(index)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (needsScrollPanel != null) {
            return needsScrollPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        tickCounter++;
        if (needsScrollPanel != null) {
            needsScrollPanel.tick();
        }
        if (--ticksUntilNextUpdate <= 0) {
            // Ask the server for an update
            PacketDistributor.sendToServer(new RequestXoonglinInfoUpdatePacket(packet.getXoonglinId()));
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