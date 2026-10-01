package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public abstract class Need {
    protected static final boolean DEFAULT_HIDDEN = false;
    protected static final boolean DEFAULT_BONUS = false;

    public static final Codec<Need> NEED_CODEC = Codec.lazyInitialized(() -> CftRegistry.NEEDS_CODEC_REGISTRY.byNameCodec()
            .dispatch("type", Need::needType, codec -> MapCodec.assumeMapUnsafe(codec)));

    private final double damage;

    private final double damageThreshold;

    /**
     * The happiness this need gives if satisfied over a full period
     * The period is given by its frequency
     */
    private final double providedHappiness;
    private final double satisfactionThreshold;
    /**
     * Given in in-game days (each day is 24000 ticks or 20 real world minutes).
     */
    private final double frequency;

    /**
     * Whether or not this need should be shown to the player. It allows for "technical" needs
     */
    private final boolean hidden;

    /**
     * Bonus needs only add happiness when satisfied, and never subtract it when unsatisfied
     */
    private final boolean bonus;

    private final ResourceLocation icon;

    public Need(double damage, double damageThreshold, double providedHappiness,
                double satisfactionThreshold, double frequency, boolean hidden, boolean bonus,
                Optional<ResourceLocation> icon) {
        this.damage = damage;
        this.damageThreshold = damageThreshold;
        this.providedHappiness = providedHappiness;
        this.satisfactionThreshold = satisfactionThreshold;
        this.frequency = frequency;
        this.hidden = hidden;
        this.bonus = bonus;
        this.icon = icon.orElse(null);
    }

    public abstract Codec<? extends Need> needType();

    public double getDamage() {
        return damage;
    }

    public double getDamageThreshold() {
        return damageThreshold;
    }

    public double getProvidedHappiness() {
        return providedHappiness;
    }

    public double getSatisfactionThreshold() {
        return satisfactionThreshold;
    }

    public double getFrequency() {
        return frequency;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isBonus() {
        return bonus;
    }

    public NeedSatisfier<? extends Need> createSatisfier() {
        return createSatisfier(this.getSatisfactionThreshold(), false);
    }

    public abstract NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied);

    public abstract List<ResourceLocation> getDefaultIcons();

    public Optional<ResourceLocation> getIcon() {
        return Optional.ofNullable(icon);
    }

    public List<ResourceLocation> getIcons() {
        if (icon != null) {
            return List.of(icon);
        }
        return getDefaultIcons();
    }

    public abstract String getTypeName();
}
