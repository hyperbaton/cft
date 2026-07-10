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
    private static final int TAB_ITEMS = 2;
    private static final int TAB_WIDTH = 60;
    private static final int TAB_HEIGHT = 14;
    private static final int TAB_Y_OFFSET = 22;
    private static final int CONTENT_Y_OFFSET = 40;

    private static final int PROGRESS_BAR_WIDTH = 60;
    private static final int PROGRESS_BAR_HEIGHT = 8;

    private static final int ARROW_SIZE = 12;

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

        if (packet.isCanMate()) {
            renderMateIndicator(graphics, x, y);
        }

        renderTabs(graphics, x, y, mouseX, mouseY);

        switch (currentTab) {
            case TAB_INFO -> renderInfoTab(graphics, x, y, mouseX, mouseY, delta);
            case TAB_JOB -> renderJobTab(graphics, x, y, mouseX, mouseY);
            case TAB_ITEMS -> renderItemsTab(graphics, x, y, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    /** A small heart shown in the top-right corner while the Xoonglin is ready to mate. */
    private void renderMateIndicator(GuiGraphics graphics, int x, int y) {
        String heart = "❤";
        int heartWidth = this.font.width(heart);
        int heartX = x + imageWidth - MARGIN_PIXELS - heartWidth;
        graphics.drawString(this.font, heart, heartX, y + MARGIN_PIXELS, 0xFFE05070, false);
    }

    private void renderTabs(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int tabY = y + TAB_Y_OFFSET;
        int tabX = x + MARGIN_PIXELS;

        renderTab(graphics, tabX, tabY, Component.translatable("gui.cft.tab_info"), currentTab == TAB_INFO);
        tabX += TAB_WIDTH + 4;

        if (packet.getJobInfo() != null) {
            renderTab(graphics, tabX, tabY, Component.translatable("gui.cft.tab_job"), currentTab == TAB_JOB);
            tabX += TAB_WIDTH + 4;
        }

        renderTab(graphics, tabX, tabY, Component.translatable("gui.cft.tab_items"), currentTab == TAB_ITEMS);
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
        int contentY = y + CONTENT_Y_OFFSET;

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

    private void renderJobTab(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        JobInfoData jobInfo = packet.getJobInfo();
        if (jobInfo == null) return;

        int contentY = y + CONTENT_Y_OFFSET;

        if (packet.getJobId() != null) {
            List<ResourceLocation> availableJobs = packet.getAvailableJobs();
            boolean canSwitch = availableJobs.size() > 1;

            String jobTranslationKey = "job." + packet.getJobId().getNamespace() + "." + packet.getJobId().getPath();
            Component jobName = Component.translatable(jobTranslationKey).withStyle(ChatFormatting.BOLD);

            if (canSwitch) {
                int arrowLeftX = x + MARGIN_PIXELS;
                int arrowRightX = x + imageWidth - MARGIN_PIXELS - ARROW_SIZE;
                int textAreaLeft = arrowLeftX + ARROW_SIZE + 4;
                int textAreaRight = arrowRightX - 4;
                int textAreaWidth = textAreaRight - textAreaLeft;
                int jobNameWidth = this.font.width(jobName);
                int jobNameX = textAreaLeft + (textAreaWidth - jobNameWidth) / 2;

                graphics.drawString(this.font, jobName, jobNameX, contentY + 2, 0x206020, false);

                boolean leftHovered = isInBounds(mouseX, mouseY, arrowLeftX, contentY, ARROW_SIZE, ARROW_SIZE);
                boolean rightHovered = isInBounds(mouseX, mouseY, arrowRightX, contentY, ARROW_SIZE, ARROW_SIZE);
                renderArrowButton(graphics, arrowLeftX, contentY, true, leftHovered);
                renderArrowButton(graphics, arrowRightX, contentY, false, rightHovered);
            } else {
                graphics.drawString(this.font, jobName, x + MARGIN_PIXELS, contentY, 0x206020, false);
            }
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

                    String valueText = entry.textValue() != null ? entry.textValue() : entry.intA() + " / " + entry.intB();
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

    private void renderArrowButton(GuiGraphics graphics, int x, int y, boolean left, boolean hovered) {
        int bg = hovered ? 0xFF606060 : 0xFF404040;
        int fg = hovered ? 0xFFFFFF : 0xC0C0C0;
        graphics.fill(x, y, x + ARROW_SIZE, y + ARROW_SIZE, bg);
        String arrow = left ? "<" : ">";
        int textWidth = this.font.width(arrow);
        graphics.drawString(this.font, arrow, x + (ARROW_SIZE - textWidth) / 2, y + 2, fg, false);
    }

    private void renderItemsTab(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int contentY = y + CONTENT_Y_OFFSET;
        List<InventorySlotData> inventory = packet.getInventoryData();

        if (inventory.isEmpty()) {
            String emptyText = Component.translatable("gui.cft.inventory_empty").getString();
            int textWidth = this.font.width(emptyText);
            graphics.drawString(this.font, emptyText,
                    x + (imageWidth - textWidth) / 2, contentY + 10, 0x808080, false);
            return;
        }

        int columns = 9;
        int slotSize = 18;
        int gridWidth = columns * slotSize;
        int gridX = x + (imageWidth - gridWidth) / 2;

        for (int i = 0; i < inventory.size(); i++) {
            InventorySlotData slot = inventory.get(i);
            int col = i % columns;
            int row = i / columns;
            int slotX = gridX + col * slotSize;
            int slotY = contentY + row * slotSize;

            graphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0xFF8B8B8B);
            graphics.fill(slotX + 1, slotY + 1, slotX + slotSize - 1, slotY + slotSize - 1, 0xFF373737);

            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(slot.item()), slot.count());
            graphics.renderItem(stack, slotX + 1, slotY + 1);
            graphics.renderItemDecorations(this.font, stack, slotX + 1, slotY + 1);
        }

        for (int i = 0; i < inventory.size(); i++) {
            InventorySlotData slot = inventory.get(i);
            int col = i % columns;
            int row = i / columns;
            int slotX = gridX + col * slotSize;
            int slotY = contentY + row * slotSize;

            if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize) {
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(slot.item()), slot.count());
                graphics.renderTooltip(this.font, stack, mouseX, mouseY);
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
        if (button == 0) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;

            // Tab clicks
            int tabY = y + TAB_Y_OFFSET;
            int tabX = x + MARGIN_PIXELS;

            if (isInBounds(mouseX, mouseY, tabX, tabY, TAB_WIDTH, TAB_HEIGHT)) {
                currentTab = TAB_INFO;
                return true;
            }
            tabX += TAB_WIDTH + 4;

            if (packet.getJobInfo() != null) {
                if (isInBounds(mouseX, mouseY, tabX, tabY, TAB_WIDTH, TAB_HEIGHT)) {
                    currentTab = TAB_JOB;
                    return true;
                }
                tabX += TAB_WIDTH + 4;
            }

            if (isInBounds(mouseX, mouseY, tabX, tabY, TAB_WIDTH, TAB_HEIGHT)) {
                currentTab = TAB_ITEMS;
                return true;
            }

            // Job arrow clicks
            if (currentTab == TAB_JOB && packet.getJobId() != null) {
                List<ResourceLocation> availableJobs = packet.getAvailableJobs();
                if (availableJobs.size() > 1) {
                    int contentY = y + CONTENT_Y_OFFSET;
                    int arrowLeftX = x + MARGIN_PIXELS;
                    int arrowRightX = x + imageWidth - MARGIN_PIXELS - ARROW_SIZE;

                    if (isInBounds(mouseX, mouseY, arrowLeftX, contentY, ARROW_SIZE, ARROW_SIZE)) {
                        switchJob(-1);
                        return true;
                    }
                    if (isInBounds(mouseX, mouseY, arrowRightX, contentY, ARROW_SIZE, ARROW_SIZE)) {
                        switchJob(1);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void switchJob(int direction) {
        List<ResourceLocation> availableJobs = packet.getAvailableJobs();
        if (availableJobs.size() <= 1 || packet.getJobId() == null) return;

        int currentIndex = availableJobs.indexOf(packet.getJobId());
        if (currentIndex < 0) currentIndex = 0;

        int newIndex = (currentIndex + direction + availableJobs.size()) % availableJobs.size();
        ResourceLocation newJobId = availableJobs.get(newIndex);

        PacketDistributor.sendToServer(new ChangeXoonglinJobPacket(packet.getXoonglinId(), newJobId));
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
                updatePacket.getJobInfo(),
                updatePacket.getInventoryData(),
                updatePacket.getAvailableJobs(),
                updatePacket.isCanMate()
        );

        if (needsScrollPanel != null) {
            needsScrollPanel.updateData(packet.getNeedsData());
        }
    }

}
