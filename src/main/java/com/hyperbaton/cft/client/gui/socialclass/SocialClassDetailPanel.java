package com.hyperbaton.cft.client.gui.socialclass;

import com.hyperbaton.cft.need.*;
import com.hyperbaton.cft.socialclass.NeedSatisfaction;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.socialclass.SocialClassUpdate;
import com.hyperbaton.cft.socialclass.SocialStructureRequirement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class SocialClassDetailPanel {

    private static final int PADDING = 8;
    private static final int LINE_HEIGHT = 12;
    private static final int SECTION_SPACING = 8;
    private static final int TAG_ROTATE_TICKS = 40;

    private static final int HEADER_COLOR = 0xFFCCAAFF;
    private static final int LABEL_COLOR = 0xFFB0B0D0;
    private static final int VALUE_COLOR = 0xFFE0E0E0;
    private static final int SECTION_HEADER_COLOR = 0xFF88AACC;
    private static final int UPGRADE_COLOR = 0xFF44AA44;
    private static final int DOWNGRADE_COLOR = 0xFFAA4444;
    private static final int PANEL_BG = 0xFF222238;

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final Font font;
    private final Registry<Need> needRegistry;

    private SocialClass selectedClass;
    private List<DetailLine> cachedLines = new ArrayList<>();
    private double scrollOffset = 0;
    private int totalContentHeight = 0;
    private int tickCounter = 0;

    public SocialClassDetailPanel(int x, int y, int width, int height, Font font, Registry<Need> needRegistry) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.font = font;
        this.needRegistry = needRegistry;
    }

    public void setSelectedClass(SocialClass socialClass) {
        this.selectedClass = socialClass;
        this.scrollOffset = 0;
        rebuildLines();
    }

    public void tick() {
        tickCounter++;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(x, y, x + width, y + height, PANEL_BG);

        if (selectedClass == null) {
            String hint = Component.translatable("gui.cft.select_class_hint").getString();
            int textWidth = font.width(hint);
            graphics.drawString(font, hint, x + (width - textWidth) / 2, y + height / 2, LABEL_COLOR, false);
            return;
        }

        graphics.enableScissor(x, y, x + width, y + height);

        int drawY = y + PADDING - (int) scrollOffset;
        for (DetailLine line : cachedLines) {
            if (drawY + LINE_HEIGHT > y && drawY < y + height) {
                line.render(graphics, font, x + PADDING + line.indent(), drawY, tickCounter);
            }
            drawY += line.lineHeight();
        }

        graphics.disableScissor();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= x && mouseX < x + width) {
            scrollOffset = Math.max(0, Math.min(scrollOffset - scrollY * 10,
                    Math.max(0, totalContentHeight - height + PADDING * 2)));
            return true;
        }
        return false;
    }

    private void rebuildLines() {
        cachedLines.clear();
        if (selectedClass == null) return;

        String className = Component.translatable(selectedClass.getId()).getString();
        cachedLines.add(new StaticLine(className, HEADER_COLOR, 0, LINE_HEIGHT + 4, true));

        if (selectedClass.getJob() != null) {
            String jobKey = "job." + selectedClass.getJob().getNamespace() + "." + selectedClass.getJob().getPath();
            addStat("gui.cft.job", Component.translatable(jobKey).getString());
        }

        cachedLines.add(new StaticLine("", 0, 0, SECTION_SPACING, false));
        cachedLines.add(new StaticLine(
                Component.translatable("gui.cft.needs").getString(), SECTION_HEADER_COLOR, 0, LINE_HEIGHT + 2, true
        ));

        for (String needId : selectedClass.getNeeds()) {
            Need need = findNeed(needId);
            if (need != null && !need.isHidden()) {
                renderNeedInfo(need);
            }
        }

        if (!selectedClass.getUpgrades().isEmpty()) {
            cachedLines.add(new StaticLine("", 0, 0, SECTION_SPACING, false));
            cachedLines.add(new StaticLine(
                    Component.translatable("gui.cft.upgrades").getString(), UPGRADE_COLOR, 0, LINE_HEIGHT + 2, true
            ));
            for (SocialClassUpdate upgrade : selectedClass.getUpgrades()) {
                renderTransition(upgrade, true);
            }
        }

        if (!selectedClass.getDowngrades().isEmpty()) {
            cachedLines.add(new StaticLine("", 0, 0, SECTION_SPACING, false));
            cachedLines.add(new StaticLine(
                    Component.translatable("gui.cft.downgrades").getString(), DOWNGRADE_COLOR, 0, LINE_HEIGHT + 2, true
            ));
            for (SocialClassUpdate downgrade : selectedClass.getDowngrades()) {
                renderTransition(downgrade, false);
            }
        }

        totalContentHeight = cachedLines.stream().mapToInt(DetailLine::lineHeight).sum();
    }

    private void addStat(String labelKey, String value) {
        String label = Component.translatable(labelKey).getString() + ": " + value;
        cachedLines.add(new StaticLine(label, LABEL_COLOR, 4, LINE_HEIGHT, false));
    }

    private void renderNeedInfo(Need need) {
        String needName = Component.translatable(need.getId()).getString();
        String typeName = getNeedTypeName(need);
        String line = needName + " (" + typeName + ")";
        List<ResourceLocation> icons = need.getIcons();
        cachedLines.add(new IconNeedLine(line, VALUE_COLOR, 4, icons));

        double freq = need.getFrequency();
        String freqText = (freq % 1 == 0) ? String.format("%.0f", freq) : String.format("%.1f", freq);
        String dayKey = (freq == 1.0) ? "gui.cft.every_x_day" : "gui.cft.every_x_days";
        String everyXDays = Component.translatable(dayKey, freqText).getString();
        cachedLines.add(new StaticLine("  " + everyXDays, LABEL_COLOR, 8, LINE_HEIGHT, false));

        if (need instanceof GoodsNeed goodsNeed) {
            Ingredient ingredient = goodsNeed.getIngredient();
            ItemStack[] items = ingredient.getItems();
            if (items.length > 0) {
                boolean isTag = items.length > 1;
                cachedLines.add(new GoodsLine(items, goodsNeed.getQuantity(), isTag, 8));
            }
        } else if (need instanceof FluidNeed fluidNeed) {
            FluidStack stack = fluidNeed.getFluidStack();
            String fluidName = stack.getDisplayName().getString();
            String text = "  " + stack.getAmount() + " mb " + fluidName;
            cachedLines.add(new StaticLine(text, LABEL_COLOR, 8, LINE_HEIGHT, false));
        }
    }

    private void renderTransition(SocialClassUpdate update, boolean isUpgrade) {
        String targetName = Component.translatable(update.getNextClass()).getString();
        int color = isUpgrade ? UPGRADE_COLOR : DOWNGRADE_COLOR;
        cachedLines.add(new StaticLine("→ " + targetName, color, 4, LINE_HEIGHT, false));

        for (NeedSatisfaction ns : update.getRequiredNeeds()) {
            String needName = Component.translatable(ns.getNeed()).getString();
            String statusKey = isUpgrade ? "gui.cft.need_satisfied" : "gui.cft.need_dissatisfied";
            String status = Component.translatable(statusKey, needName).getString();
            cachedLines.add(new StaticLine("  " + status, LABEL_COLOR, 8, LINE_HEIGHT, false));
        }

        for (SocialStructureRequirement req : update.getSocialStructureRequirements()) {
            String className = Component.translatable(req.getSocialClass()).getString();
            cachedLines.add(new StaticLine(
                    "  " + className + " ≥ " + String.format("%.0f%%", req.getPercentage() * 100)
                            + " " + Component.translatable("gui.cft.of_population").getString(),
                    LABEL_COLOR, 8, LINE_HEIGHT, false
            ));
        }
    }

    private Need findNeed(String needId) {
        for (Need need : needRegistry) {
            if (need.getId().equals(needId)) {
                return need;
            }
        }
        return null;
    }

    private String getNeedTypeName(Need need) {
        return need.getTypeName();
    }

    private interface DetailLine {
        void render(GuiGraphics graphics, Font font, int x, int y, int tickCounter);
        int indent();
        int lineHeight();
    }

    private record StaticLine(String text, int color, int indent, int lineHeight, boolean bold) implements DetailLine {
        @Override
        public void render(GuiGraphics graphics, Font font, int x, int y, int tickCounter) {
            if (!text.isEmpty()) {
                graphics.drawString(font, text, x, y, color, bold);
            }
        }
    }

    private record GoodsLine(ItemStack[] items, int quantity, boolean isTag, int indent) implements DetailLine {
        @Override
        public void render(GuiGraphics graphics, Font font, int x, int y, int tickCounter) {
            int index = isTag ? (tickCounter / TAG_ROTATE_TICKS) % items.length : 0;
            String itemName = items[index].getHoverName().getString();
            String prefix = "  " + quantity + "x ";

            if (isTag) {
                String text = prefix + "#" + itemName;
                graphics.drawString(font, text, x, y, LABEL_COLOR, false);
            } else {
                graphics.drawString(font, prefix + itemName, x, y, LABEL_COLOR, false);
            }
        }

        @Override
        public int lineHeight() {
            return LINE_HEIGHT;
        }
    }

    private record IconNeedLine(String text, int color, int indent, List<ResourceLocation> icons) implements DetailLine {
        private static final int ICON_SIZE = 16;

        @Override
        public void render(GuiGraphics graphics, Font font, int x, int y, int tickCounter) {
            if (!icons.isEmpty()) {
                int index = icons.size() > 1 ? (tickCounter / TAG_ROTATE_TICKS) % icons.size() : 0;
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(icons.get(index)));
                graphics.renderItem(stack, x, y - 1);
            }
            graphics.drawString(font, text, x + ICON_SIZE + 2, y + 3, color, false);
        }

        @Override
        public int lineHeight() {
            return Math.max(LINE_HEIGHT, ICON_SIZE + 2);
        }
    }
}
