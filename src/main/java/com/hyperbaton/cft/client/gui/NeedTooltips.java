package com.hyperbaton.cft.client.gui;

import com.hyperbaton.cft.event.CftDatapackRegistryEvents;
import com.hyperbaton.cft.need.Need;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the hover tooltip for a need: its name, an optional hand-written description
 * from the lang files ({@code need.<namespace>.<path>.tooltip}) and a summary generated
 * from the need definition, so datapack needs get a useful tooltip even without lang entries.
 */
public class NeedTooltips {
    private static final int MAX_WIDTH = 200;

    public static List<FormattedCharSequence> build(Font font, ResourceLocation needId, @Nullable String extraTooltip) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable(needId.toString()).withStyle(ChatFormatting.YELLOW).getVisualOrderText());

        String descriptionKey = descriptionKey(needId);
        if (I18n.exists(descriptionKey)) {
            lines.addAll(font.split(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY), MAX_WIDTH));
        }

        Need need = findNeed(needId);
        if (need != null) {
            lines.add(Component.translatable("gui.cft.need_tooltip.type", need.getTypeName())
                    .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            lines.add(Component.translatable(need.isBonus() ? "gui.cft.need_tooltip.bonus_happiness" : "gui.cft.need_tooltip.happiness",
                            formatNumber(need.getProvidedHappiness()), formatNumber(need.getFrequency()))
                    .withStyle(ChatFormatting.GREEN).getVisualOrderText());
            if (need.getDamage() > 0.0) {
                lines.add(Component.translatable("gui.cft.need_tooltip.harmful")
                        .withStyle(ChatFormatting.RED).getVisualOrderText());
            }
        }

        if (extraTooltip != null) {
            lines.addAll(font.split(Component.literal(extraTooltip).withStyle(ChatFormatting.AQUA), MAX_WIDTH));
        }
        return lines;
    }

    private static String descriptionKey(ResourceLocation needId) {
        return "need." + needId.getNamespace() + "." + needId.getPath() + ".tooltip";
    }

    @Nullable
    private static Need findNeed(ResourceLocation needId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        return minecraft.level.registryAccess().registry(CftDatapackRegistryEvents.NEED_KEY)
                .map(registry -> registry.get(needId))
                .orElse(null);
    }

    private static String formatNumber(double value) {
        return value % 1 == 0 ? String.format("%.0f", value) : String.format("%.1f", value);
    }
}
