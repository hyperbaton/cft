package com.hyperbaton.cft.client.gui.socialclass;

import com.hyperbaton.cft.client.gui.NeedTooltips;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.socialclass.CensusStats;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.util.LangUtil;
import com.hyperbaton.cft.world.PopulationSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.*;

/**
 * Census of the leader's Xoonglins: population and happiness per class, how the
 * population evolved over the last days, which needs are failing and what everyone works on.
 */
public class CensusPanel {

    private static final int PADDING = 10;
    private static final int COLUMN_GAP = 16;
    private static final int LINE_HEIGHT = 12;
    private static final int ROW_HEIGHT = 18;
    private static final int SECTION_SPACING = 10;
    private static final int CHART_HEIGHT = 56;
    private static final int HAPPINESS_BAR_WIDTH = 60;
    private static final int ICON_ROTATE_TICKS = 40;
    private static final int MAX_NEED_ISSUES = 10;

    private static final int PANEL_BG = 0xFF222238;
    private static final int SECTION_HEADER_COLOR = 0xFF88AACC;
    private static final int LABEL_COLOR = 0xFFB0B0D0;
    private static final int VALUE_COLOR = 0xFFE0E0E0;
    private static final int MUTED_COLOR = 0xFF707090;
    private static final int UP_COLOR = 0xFF44AA44;
    private static final int DOWN_COLOR = 0xFFAA4444;
    private static final int WARNING_COLOR = 0xFFE0B040;
    private static final int CHART_BG = 0xFF1A1A2E;
    private static final int CHART_BAR = 0xFF4A6FA5;
    private static final int CHART_BAR_LIVE = 0xFF88AACC;
    private static final int BAR_BG = 0xFF3A3A55;

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final Font font;
    private final Registry<SocialClass> socialClassRegistry;
    private final Registry<Need> needRegistry;

    private CensusStats stats;
    private double scrollOffset = 0;
    private int contentHeight = 0;
    private int tickCounter = 0;
    private ResourceLocation hoveredNeedId;

    public CensusPanel(int x, int y, int width, int height, Font font,
                                Registry<SocialClass> socialClassRegistry, Registry<Need> needRegistry) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.font = font;
        this.socialClassRegistry = socialClassRegistry;
        this.needRegistry = needRegistry;
    }

    public void setStats(CensusStats stats) {
        this.stats = stats;
    }

    public void tick() {
        tickCounter++;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(x, y, x + width, y + height, PANEL_BG);
        hoveredNeedId = null;

        if (stats == null) {
            drawCentered(graphics, Component.translatable("gui.cft.census.loading").getString(), LABEL_COLOR);
            return;
        }
        if (stats.totalPopulation() == 0 && stats.history().isEmpty()) {
            drawCentered(graphics, Component.translatable("gui.cft.census.no_xoonglins").getString(), LABEL_COLOR);
            return;
        }

        int columnWidth = (width - 2 * PADDING - COLUMN_GAP) / 2;
        int leftX = x + PADDING;
        int rightX = leftX + columnWidth + COLUMN_GAP;
        int top = y + PADDING - (int) scrollOffset;

        graphics.enableScissor(x, y, x + width, y + height);
        int leftBottom = renderLeftColumn(graphics, leftX, top, columnWidth);
        int rightBottom = renderRightColumn(graphics, rightX, top, columnWidth, mouseX, mouseY);
        graphics.disableScissor();

        contentHeight = Math.max(leftBottom, rightBottom) - top + PADDING;
    }

    /** Rendered after the rest of the screen so the tooltip isn't clipped by the panel. */
    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredNeedId != null) {
            graphics.renderTooltip(font, NeedTooltips.build(font, hoveredNeedId, null), mouseX, mouseY);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            scrollOffset = Math.max(0, Math.min(scrollOffset - scrollY * 10, Math.max(0, contentHeight - height)));
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- left column

    private int renderLeftColumn(GuiGraphics graphics, int colX, int drawY, int colWidth) {
        PopulationSnapshot latest = stats.history().isEmpty() ? null : stats.history().get(stats.history().size() - 1);

        drawY = drawHeader(graphics, "gui.cft.census.overview", colX, drawY);
        int total = stats.totalPopulation();
        String populationText = Component.translatable("gui.cft.census.population", total).getString();
        graphics.drawString(font, populationText, colX + 4, drawY, VALUE_COLOR, false);
        if (latest != null) {
            drawDelta(graphics, total - latest.total(), colX + 4 + font.width(populationText) + 6, drawY, true);
        }
        drawY += LINE_HEIGHT;
        graphics.drawString(font, Component.translatable("gui.cft.census.average_happiness",
                String.format("%.1f", overallAverageHappiness())).getString(), colX + 4, drawY, VALUE_COLOR, false);
        drawY += LINE_HEIGHT + SECTION_SPACING;

        drawY = drawHeader(graphics, "gui.cft.census.population_trend", colX, drawY);
        drawY = renderPopulationChart(graphics, colX + 4, drawY, colWidth - 4);
        drawY += SECTION_SPACING;

        drawY = drawHeader(graphics, "gui.cft.census.classes", colX, drawY);
        return renderClassTable(graphics, colX + 4, drawY, colWidth - 4, latest);
    }

    private int renderPopulationChart(GuiGraphics graphics, int chartX, int drawY, int chartWidth) {
        List<Integer> totals = new ArrayList<>();
        stats.history().forEach(snapshot -> totals.add(snapshot.total()));
        totals.add(stats.totalPopulation());

        int labelWidth = font.width(String.valueOf(totals.stream().mapToInt(Integer::intValue).max().orElse(0))) + 4;
        int barsX = chartX + labelWidth;
        int barsWidth = chartWidth - labelWidth;
        int max = Math.max(1, totals.stream().mapToInt(Integer::intValue).max().orElse(1));

        graphics.fill(barsX, drawY, barsX + barsWidth, drawY + CHART_HEIGHT, CHART_BG);
        graphics.drawString(font, String.valueOf(max), chartX, drawY, MUTED_COLOR, false);
        graphics.drawString(font, "0", chartX, drawY + CHART_HEIGHT - 8, MUTED_COLOR, false);

        int slot = Math.max(1, barsWidth / Math.max(totals.size(), 1));
        int barWidth = Math.max(1, slot - 1);
        for (int i = 0; i < totals.size(); i++) {
            int barHeight = Math.round((float) totals.get(i) / max * (CHART_HEIGHT - 2));
            int bx = barsX + i * slot;
            int color = i == totals.size() - 1 ? CHART_BAR_LIVE : CHART_BAR;
            graphics.fill(bx, drawY + CHART_HEIGHT - barHeight, bx + barWidth, drawY + CHART_HEIGHT, color);
        }
        drawY += CHART_HEIGHT + 2;

        if (totals.size() > 1) {
            String left = Component.translatable("gui.cft.census.days_ago", totals.size() - 1).getString();
            graphics.drawString(font, left, barsX, drawY, MUTED_COLOR, false);
        }
        String right = Component.translatable("gui.cft.census.now").getString();
        graphics.drawString(font, right, barsX + barsWidth - font.width(right), drawY, MUTED_COLOR, false);
        return drawY + LINE_HEIGHT;
    }

    private int renderClassTable(GuiGraphics graphics, int tableX, int drawY, int tableWidth, PopulationSnapshot latest) {
        Set<ResourceLocation> classIds = new HashSet<>(stats.population().keySet());
        if (latest != null) classIds.addAll(latest.classCounts().keySet());
        List<ResourceLocation> sorted = new ArrayList<>(classIds);
        sorted.sort(Comparator.comparingInt((ResourceLocation id) -> stats.population().getOrDefault(id, 0)).reversed()
                .thenComparing(id -> LangUtil.socialClassName(id).getString()));

        int barX = tableX + tableWidth - HAPPINESS_BAR_WIDTH;
        int countRightX = barX - 38;
        int deltaX = barX - 32;
        for (ResourceLocation classId : sorted) {
            int count = stats.population().getOrDefault(classId, 0);
            String name = font.plainSubstrByWidth(LangUtil.socialClassName(classId).getString(), countRightX - tableX - 18);
            graphics.drawString(font, name, tableX, drawY, count > 0 ? VALUE_COLOR : MUTED_COLOR, false);

            String countText = String.valueOf(count);
            graphics.drawString(font, countText, countRightX - font.width(countText), drawY, VALUE_COLOR, false);
            if (latest != null) {
                drawDelta(graphics, count - latest.classCounts().getOrDefault(classId, 0), deltaX, drawY, false);
            }

            if (count > 0) {
                double happiness = stats.averageHappiness().getOrDefault(classId, 0.0);
                double maxHappiness = findClass(classId).map(SocialClass::getMaxHappiness).orElse(100.0);
                renderHappinessBar(graphics, barX, drawY, happiness, maxHappiness);
            }
            drawY += LINE_HEIGHT;
        }
        return drawY;
    }

    private void renderHappinessBar(GuiGraphics graphics, int barX, int drawY, double happiness, double maxHappiness) {
        float fraction = (float) Math.max(0, Math.min(1, maxHappiness > 0 ? happiness / maxHappiness : 0));
        int filled = Math.round(fraction * HAPPINESS_BAR_WIDTH);
        int color = fraction >= 0.5f ? UP_COLOR : fraction >= 0.2f ? WARNING_COLOR : DOWN_COLOR;
        graphics.fill(barX, drawY, barX + HAPPINESS_BAR_WIDTH, drawY + 8, BAR_BG);
        graphics.fill(barX, drawY, barX + filled, drawY + 8, color);
        String value = String.format("%.0f", happiness);
        graphics.drawString(font, value, barX + (HAPPINESS_BAR_WIDTH - font.width(value)) / 2, drawY, VALUE_COLOR, true);
    }

    // --------------------------------------------------------------- right column

    private int renderRightColumn(GuiGraphics graphics, int colX, int drawY, int colWidth, int mouseX, int mouseY) {
        drawY = drawHeader(graphics, "gui.cft.census.need_issues", colX, drawY);
        if (stats.needIssues().isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.cft.census.no_need_issues").getString(),
                    colX + 4, drawY, UP_COLOR, false);
            drawY += LINE_HEIGHT;
        } else {
            for (CensusStats.NeedIssue issue : stats.needIssues().subList(0, Math.min(MAX_NEED_ISSUES, stats.needIssues().size()))) {
                drawY = renderNeedIssue(graphics, issue, colX + 4, drawY, colWidth - 4, mouseX, mouseY);
            }
            if (stats.needIssues().size() > MAX_NEED_ISSUES) {
                graphics.drawString(font, Component.translatable("gui.cft.census.more",
                        stats.needIssues().size() - MAX_NEED_ISSUES).getString(), colX + 4, drawY, MUTED_COLOR, false);
                drawY += LINE_HEIGHT;
            }
        }
        drawY += SECTION_SPACING;

        drawY = drawHeader(graphics, "gui.cft.census.jobs", colX, drawY);
        return renderJobs(graphics, colX + 4, drawY, colWidth - 4);
    }

    private int renderNeedIssue(GuiGraphics graphics, CensusStats.NeedIssue issue, int rowX, int drawY, int rowWidth,
                                int mouseX, int mouseY) {
        int textX = rowX;
        Need need = findNeed(issue.needId());
        if (need != null && !need.getIcons().isEmpty()) {
            List<ResourceLocation> icons = need.getIcons();
            ResourceLocation icon = icons.get(icons.size() > 1 ? (tickCounter / ICON_ROTATE_TICKS) % icons.size() : 0);
            graphics.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(icon)), rowX, drawY);
            textX += 18;
        }

        String critical = issue.critical() > 0
                ? Component.translatable("gui.cft.census.critical", issue.critical()).getString()
                : "";
        String unsatisfied = String.valueOf(issue.unsatisfied());
        int rightX = rowX + rowWidth;
        int criticalX = rightX - font.width(critical);
        int unsatisfiedX = criticalX - (critical.isEmpty() ? 0 : 6) - font.width(unsatisfied);

        String name = font.plainSubstrByWidth(LangUtil.needName(issue.needId()).getString(), unsatisfiedX - textX - 4);
        graphics.drawString(font, name, textX, drawY + 4, VALUE_COLOR, false);
        graphics.drawString(font, unsatisfied, unsatisfiedX, drawY + 4, WARNING_COLOR, false);
        if (!critical.isEmpty()) {
            graphics.drawString(font, critical, criticalX, drawY + 4, DOWN_COLOR, false);
        }

        if (mouseX >= rowX && mouseX < rightX && mouseY >= drawY && mouseY < drawY + ROW_HEIGHT
                && mouseY >= y && mouseY < y + height) {
            hoveredNeedId = issue.needId();
        }
        return drawY + ROW_HEIGHT;
    }

    private int renderJobs(GuiGraphics graphics, int rowX, int drawY, int rowWidth) {
        List<Map.Entry<String, Integer>> jobs = new ArrayList<>(stats.jobs().entrySet());
        jobs.sort(Comparator.comparing((Map.Entry<String, Integer> entry) -> entry.getKey().equals(CensusStats.NO_JOB))
                .thenComparing(Map.Entry.<String, Integer>comparingByValue().reversed()));
        for (Map.Entry<String, Integer> job : jobs) {
            boolean noJob = job.getKey().equals(CensusStats.NO_JOB);
            String name = noJob
                    ? Component.translatable("gui.cft.census.no_job").getString()
                    : LangUtil.jobName(ResourceLocation.parse(job.getKey())).getString();
            String count = String.valueOf(job.getValue());
            name = font.plainSubstrByWidth(name, rowWidth - font.width(count) - 8);
            graphics.drawString(font, name, rowX, drawY, noJob ? MUTED_COLOR : VALUE_COLOR, false);
            graphics.drawString(font, count, rowX + rowWidth - font.width(count), drawY, VALUE_COLOR, false);
            drawY += LINE_HEIGHT;
        }
        return drawY;
    }

    // ------------------------------------------------------------------- helpers

    private int drawHeader(GuiGraphics graphics, String key, int drawX, int drawY) {
        graphics.drawString(font, Component.translatable(key).getString(), drawX, drawY, SECTION_HEADER_COLOR, true);
        return drawY + LINE_HEIGHT + 2;
    }

    private void drawDelta(GuiGraphics graphics, int delta, int drawX, int drawY, boolean withLabel) {
        if (delta == 0 && !withLabel) return;
        String text = (delta > 0 ? "+" : "") + delta;
        if (withLabel) {
            text = Component.translatable("gui.cft.census.delta_today", text).getString();
        }
        int color = delta > 0 ? UP_COLOR : delta < 0 ? DOWN_COLOR : MUTED_COLOR;
        graphics.drawString(font, text, drawX, drawY, color, false);
    }

    private void drawCentered(GuiGraphics graphics, String text, int color) {
        graphics.drawString(font, text, x + (width - font.width(text)) / 2, y + height / 2, color, false);
    }

    private double overallAverageHappiness() {
        double sum = 0;
        int total = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : stats.population().entrySet()) {
            sum += stats.averageHappiness().getOrDefault(entry.getKey(), 0.0) * entry.getValue();
            total += entry.getValue();
        }
        return total == 0 ? 0 : sum / total;
    }

    private Optional<SocialClass> findClass(ResourceLocation classId) {
        return Optional.ofNullable(socialClassRegistry.get(classId));
    }

    private Need findNeed(ResourceLocation needId) {
        return needRegistry.get(needId);
    }
}
