package com.hyperbaton.cft.capability.need;

import com.hyperbaton.cft.CftRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Function;

public abstract class Need {

    public static final Codec<Need> NEED_CODEC = ExtraCodecs.lazyInitializedCodec(() -> CftRegistry.NEEDS_CODEC_SUPPLIER.get().getCodec()
            .dispatch(Need::needType, Function.identity()));

    private final String id;
    private double damage;

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

    public static final String TAG_ID = "id";
    public static final String TAG_DAMAGE = "damage";
    public static final String TAG_PROVIDED_HAPPINESS = "providedHappiness";
    public static final String TAG_SATISFACTION_THRESHOLD = "satisfactionThreshold";
    public static final String TAG_FREQUENCY = "frequency";

    public Need(String id, double damage, double providedHappiness, double satisfactionThreshold, double frequency) {
        this.id = id;
        this.damage = damage;
        this.providedHappiness = providedHappiness;
        this.satisfactionThreshold = satisfactionThreshold;
        this.frequency = frequency;
    }

    public String getId() {
        return id;
    }

    public abstract Codec<?extends Need> needType();

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
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

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ID, id);
        tag.putDouble(TAG_DAMAGE, damage);
        tag.putDouble(TAG_PROVIDED_HAPPINESS, providedHappiness);
        tag.putDouble(TAG_SATISFACTION_THRESHOLD, satisfactionThreshold);
        tag.putDouble(TAG_FREQUENCY, frequency);
        return tag;
    }
}
