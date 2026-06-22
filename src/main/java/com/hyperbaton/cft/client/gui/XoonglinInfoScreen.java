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

    private static final int TAB_INFO = 0;
    private static final int TAB_JOB = 1;
    private static final int TAB_WIDTH = 60;
    private static final int TAB_HEIGHT = 14;
    private static final int TAB_Y_OFFSET = 22;
    private static final int CONTENT_Y_OFFSET = 40;

    private static final int PROGRESS_BAR_WIDTH = 60;
    private static final int PROGRESS_BAR_HEIGHT = 8;

    private final int imageWidth = 220, imageHeight = 176;
    private CheckOnXoonglinPacket packet;
    private int ticksUntilNextUpdate = UPDATE_FREQUENCY;
    private int tickCounter = 0;
    private int currentTab = TAB_INFO;

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
                    y + CONTENT_Y_OFFSET + 25,
                    x + MARGIN_PIXELS,
                    packet.getNeedsData()
            );
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (this.minecraft == null) {
            this.minecraft = Minecraft.getInstance();
        }

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 220, 176);

        Component titleText = Component.literal(this.title.getString()).withStyle(ChatFormatting.BOLD);
        int titleWidth = this.font.width(titleText);
        int titleX = x + (imageWidth / 2) - (titleWidth / 2);
        graphics.drawString(this.font, titleText, titleX, y + MARGIN_PIXELS, 0x4040B0, false);

        boolean hasJob = packet.getJobInfo() != null;
        if (hasJob) {
            renderTabs(graphics, x, y, mouseX, mouseY);
        }

        if (currentTab == TAB_INFO || !hasJob) {
            renderInfoTab(graphics, x, y, mouseX, mouseY, delta);
        } else {
            renderJobTab(graphics, x, y);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderTabs(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int tabY = y + TAB_Y_OFFSET;
        int tab1X = x + MARGIN_PIXELS;
        int tab2X = tab1X + TAB_WIDTH + 4;

        renderTab(graphics, tab1X, tabY, Component.translatable("gui.cft.tab_info"), currentTab == TAB_INFO);
        renderTab(graphics, tab2X, tabY, Component.translatable("gui.cft.tab_job"), currentTab == TAB_JOB);
    }

    private void renderTab(GuiGraphics graphics, int x, int y, Component label, boolean selected) {
        int fill = selected ? 0xFF898989 : 0xFF555555;
        int highlight = selected ? 0xFFCFCFCF : 0xFF939393;
        int shadow = selected ? 0xFF373737 : 0xFF252525;
        int textColor = selected ? 0xFFFFFF : 0xA0A0A0;

        graphics.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, fill);
        graphics.fill(x, y, x + TAB_WIDTH, y + 1, highlight);
        graphics.fill(x, y, x + 1, y + TAB_HEIGHT, highlight);
        graphics.fill(x + TAB_WIDTH - 1, y, x + TAB_WIDTH, y + TAB_HEIGHT, shadow);
        graphics.fill(x, y + TAB_HEIGHT - 1, x + TAB_WIDTH, y + TAB_HEIGHT, shadow);
        graphics.fill(x + 1, y + 1, x + TAB_WIDTH - 1, y + 2, 0x30FFFFFF);

        String text = label.getString();
        int textWidth = this.font.width(text);
        graphics.drawString(this.font, text, x + (TAB_WIDTH - textWidth) / 2, y + 3, textColor, false);
    }

    private void renderInfoTab(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float delta) {
        int contentY = packet.getJobInfo() != null ? y + CONTENT_Y_OFFSET : y + 30;

        renderSocialClassAndJob(graphics, x, contentY);

        String happinessLabel = Component.translatable("gui.cft.happiness").getString();
        String happinessValue = String.format("%.2f", packet.getHappiness());
        graphics.drawString(this.font, happinessLabel, x + MARGIN_PIXELS, contentY + 15, 0x404040, false);
        int happinessLabelWidth = this.font.width(happinessValue);
        int happinessLabelRightX = x + imageWidth - MARGIN_PIXELS - happinessLabelWidth;
        graphics.drawString(this.font, happinessValue, happinessLabelRightX, contentY + 15, 0x404040, false);

        int needsStartY = contentY + 35;
        if (packet.getNeedsData().size() <= MAX_VISIBLE_NEEDS) {
            renderNeedsNormally(graphics, x, needsStartY, mouseX, mouseY);
        } else {
            needsScrollPanel.render(graphics, mouseX, mouseY, delta);
            Component tooltip = needsScrollPanel.getCurrentTooltip();
            if (tooltip != null) {
                graphics.renderTooltip(this.font, tooltip,
                        needsScrollPanel.getTooltipX(),
                        needsScrollPanel.getTooltipY());
            }
        }
    }

    private void renderJobTab(GuiGraphics graphics, int x, int y) {
        JobInfoData jobInfo = packet.getJobInfo();
        if (jobInfo == null) return;

        int contentY = y + CONTENT_Y_OFFSET;

        if (packet.getJobId() != null) {
            String jobTranslationKey = "job." + packet.getJobId().getNamespace() + "." + packet.getJobId().getPath();
            Component jobName = Component.translatable(jobTranslationKey).withStyle(ChatFormatting.BOLD);
            graphics.drawString(this.font, jobName, x + MARGIN_PIXELS, contentY, 0x206020, false);
            contentY += 14;
        }

        String statusText = Component.translatable(jobInfo.statusKey()).getString();
        graphics.drawString(this.font, statusText, x + MARGIN_PIXELS, contentY, jobInfo.statusColor(), false);
        contentY += 16;

        for (JobDisplayEntry entry : jobInfo.entries()) {
            switch (entry.type()) {
                case JobDisplayEntry.TEXT -> {
                    String label = Component.translatable(entry.labelKey()).getString();
                    graphics.drawString(this.font, label, x + MARGIN_PIXELS, contentY, 0x404040, false);
                    int valueWidth = this.font.width(entry.textValue());
                    graphics.drawString(this.font, entry.textValue(),
                            x + imageWidth - MARGIN_PIXELS - valueWidth, contentY, entry.intA(), false);
                    contentY += 14;
                }
                case JobDisplayEntry.PROGRESS -> {
                    String label = Component.translatable(entry.labelKey()).getString();
                    graphics.drawString(this.font, label, x + MARGIN_PIXELS, contentY, 0x404040, false);

                    int barX = x + imageWidth - MARGIN_PIXELS - PROGRESS_BAR_WIDTH;
                    renderProgressBar(graphics, barX, contentY, entry.intA(), entry.intB());

                    String valueText = entry.intA() + " / " + entry.intB();
                    int valueWidth = this.font.width(valueText);
                    graphics.drawString(this.font, valueText,
                            barX + (PROGRESS_BAR_WIDTH - valueWidth) / 2, contentY, 0xFFFFFF, true);
                    contentY += 14;
                }
                case JobDisplayEntry.ITEM -> {
                    String label = Component.translatable(entry.labelKey()).getString();
                    graphics.drawString(this.font, label, x + MARGIN_PIXELS, contentY, 0x404040, false);

                    ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(entry.icon()));
                    int itemX = x + imageWidth - MARGIN_PIXELS - ICON_SIZE - 30;
                    graphics.renderItem(stack, itemX, contentY - 4);
                    String countText = "x" + entry.intA();
                    graphics.drawString(this.font, countText, itemX + ICON_SIZE + 2, contentY, 0x404040, false);
                    contentY += 18;
                }
            }
        }
    }

    private void renderProgressBar(GuiGraphics graphics, int x, int y, int current, int max) {
        graphics.fill(x, y, x + PROGRESS_BAR_WIDTH, y + PROGRESS_BAR_HEIGHT, 0xFF303030);
        if (max > 0) {
            int filledWidth = Math.min(PROGRESS_BAR_WIDTH, (int) ((double) current / max * PROGRESS_BAR_WIDTH));
            int color = current >= max ? 0xFF40AA40 : 0xFF4080DD;
            graphics.fill(x, y, x + filledWidth, y + PROGRESS_BAR_HEIGHT, color);
        }
        graphics.fill(x, y, x + PROGRESS_BAR_WIDTH, y + 1, 0xFF505050);
        graphics.fill(x, y + PROGRESS_BAR_HEIGHT - 1, x + PROGRESS_BAR_WIDTH, y + PROGRESS_BAR_HEIGHT, 0xFF505050);
        graphics.fill(x, y, x + 1, y + PROGRESS_BAR_HEIGHT, 0xFF505050);
        graphics.fill(x + PROGRESS_BAR_WIDTH - 1, y, x + PROGRESS_BAR_WIDTH, y + PROGRESS_BAR_HEIGHT, 0xFF505050);
    }

    private void renderSocialClassAndJob(GuiGraphics graphics, int x, int y) {
        String socialClass = Component.translatable(packet.getSocialClass()).getString();
        graphics.drawString(this.font, socialClass, x + MARGIN_PIXELS, y, 0x404040, false);

        if (packet.getJobId() != null) {
            String jobTranslationKey = "job." + packet.getJobId().getNamespace() + "." + packet.getJobId().getPath();
            String jobName = Component.translatable(jobTranslationKey).getString();
            int jobNameWidth = this.font.width(jobName);
            int jobNameRightX = x + imageWidth - MARGIN_PIXELS - jobNameWidth;
            graphics.drawString(this.font, jobName, jobNameRightX, y, 0x206020, false);
        }
    }

    private void renderNeedsNormally(GuiGraphics graphics, int x, int startY, int mouseX, int mouseY) {
        int barY = startY;
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && packet.getJobInfo() != null) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;
            int tabY = y + TAB_Y_OFFSET;
            int tab1X = x + MARGIN_PIXELS;
            int tab2X = tab1X + TAB_WIDTH + 4;

            if (isInBounds(mouseX, mouseY, tab1X, tabY, TAB_WIDTH, TAB_HEIGHT)) {
                currentTab = TAB_INFO;
                return true;
            }
            if (isInBounds(mouseX, mouseY, tab2X, tabY, TAB_WIDTH, TAB_HEIGHT)) {
                currentTab = TAB_JOB;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentTab == TAB_INFO && needsScrollPanel != null) {
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
                updatePacket.getXoonglinId(),
                updatePacket.getJobInfo()
        );

        if (needsScrollPanel != null) {
            needsScrollPanel.updateData(packet.getNeedsData());
        }
    }

}
