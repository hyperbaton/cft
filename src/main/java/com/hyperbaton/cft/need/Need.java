package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Function;

public abstract class Need {
    protected static final boolean DEFAULT_HIDDEN = false;

    public static final Codec<Need> NEED_CODEC = ExtraCodecs.lazyInitializedCodec(() -> CftRegistry.NEEDS_CODEC_SUPPLIER.get().getCodec()
            .dispatch(Need::needType, Function.identity()));

    private final String id;
    private double damage;

    private double damageThreshold;

    /**
     * The happiness this need gives if satisfied over a full period
     * The period is given by its frequency
     */
    private double providedHappiness;
    private double satisfactionThreshold;
    /**
     * Given in in-game days (each day is 24000 ticks or 20 real world minutes).
     */
    private double frequency;

    /**
     * Whether or not this need should be shown to the player. It allows for "technical" needs
     */
    private boolean hidden;

    public static final String TAG_ID = "id";
    public static final String TAG_DAMAGE = "damage";
    public static final String TAG_DAMAGE_THRESHOLD = "damageThreshold";
    public static final String TAG_PROVIDED_HAPPINESS = "providedHappiness";
    public static final String TAG_SATISFACTION_THRESHOLD = "satisfactionThreshold";
    public static final String TAG_FREQUENCY = "frequency";
    public static final String TAG_HIDDEN = "hidden";

    public Need(String id, double damage, double damageThreshold, double providedHappiness,
                double satisfactionThreshold, double frequency, boolean hidden) {
        this.id = id;
        this.damage = damage;
        this.damageThreshold = damageThreshold;
        this.providedHappiness = providedHappiness;
        this.satisfactionThreshold = satisfactionThreshold;
        this.frequency = frequency;
        this.hidden = hidden;
    }

    public String getId() {
        return id;
    }

    public abstract Codec<? extends Need> needType();

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getDamageThreshold() {
        return damageThreshold;
    }

    public void setDamageThreshold(double damageThreshold) {
        this.damageThreshold = damageThreshold;
    }

    public double getProvidedHappiness() {
        return providedHappiness;
    }

    public void setProvidedHappiness(double providedHappiness) {
        this.providedHappiness = providedHappiness;
    }

    public double getSatisfactionThreshold() {
        return satisfactionThreshold;
    }

    public void setSatisfactionThreshold(double satisfactionThreshold) {
        this.satisfactionThreshold = satisfactionThreshold;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public NeedSatisfier<? extends Need> createSatisfier() {
        return createSatisfier(this.getSatisfactionThreshold(), false);
    }

    public abstract NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied);

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ID, id);
        tag.putDouble(TAG_DAMAGE, damage);
        tag.putDouble(TAG_DAMAGE_THRESHOLD, damageThreshold);
        tag.putDouble(TAG_PROVIDED_HAPPINESS, providedHappiness);
        tag.putDouble(TAG_SATISFACTION_THRESHOLD, satisfactionThreshold);
        tag.putDouble(TAG_FREQUENCY, frequency);
        tag.putBoolean(TAG_HIDDEN, hidden);
        return tag;
    }
}
