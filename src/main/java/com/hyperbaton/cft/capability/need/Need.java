package com.hyperbaton.cft.capability.need;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public abstract class Need {

    private final String id;
    private String needType;
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
    public static final String TAG_NEED_TYPE = "needType";
    public static final String TAG_DAMAGE = "damage";
    public static final String TAG_PROVIDED_HAPPINESS = "providedHappiness";
    public static final String TAG_SATISFACTION_THRESHOLD = "satisfactionThreshold";
    public static final String TAG_FREQUENCY = "frequency";

    public Need(String id, String needType, double damage, double providedHappiness, double satisfactionThreshold, double frequency) {
        this.id = id;
        this.needType = needType;
        this.damage = damage;
        this.providedHappiness = providedHappiness;
        this.satisfactionThreshold = satisfactionThreshold;
        this.frequency = frequency;
    }

    public String getId() {
        return id;
    }

    public String getNeedType() {
        return needType;
    }

    public void setNeedType(String needType) {
        this.needType = needType;
    }

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
        tag.putString(TAG_NEED_TYPE, needType);
        tag.putDouble(TAG_DAMAGE, damage);
        tag.putDouble(TAG_PROVIDED_HAPPINESS, providedHappiness);
        tag.putDouble(TAG_SATISFACTION_THRESHOLD, satisfactionThreshold);
        tag.putDouble(TAG_FREQUENCY, frequency);
        return tag;
    }
}
