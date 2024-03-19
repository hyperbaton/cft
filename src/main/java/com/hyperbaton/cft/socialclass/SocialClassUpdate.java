package com.hyperbaton.cft.socialclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class SocialClassUpdate {

    public static final Codec<SocialClassUpdate> GOODS_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("nextClass").forGetter(SocialClassUpdate::getNextClass),
            Codec.DOUBLE.fieldOf("requiredHappiness").forGetter(SocialClassUpdate::getRequiredHappiness),
            NeedSatisfaction.NEED_SATISFACTION_CODEC.listOf().fieldOf("requiredNeeds").forGetter(SocialClassUpdate::getRequiredNeeds)
    ).apply(instance, SocialClassUpdate::new));
    private String nextClass;
    private double requiredHappiness;
    List<NeedSatisfaction> requiredNeeds;

    public SocialClassUpdate(String nextClass, double requiredHappiness, List<NeedSatisfaction> requiredNeeds) {
        this.nextClass = nextClass;
        this.requiredHappiness = requiredHappiness;
        this.requiredNeeds = requiredNeeds;
    }

    public String getNextClass() {
        return nextClass;
    }

    public void setNextClass(String nextClass) {
        this.nextClass = nextClass;
    }

    public double getRequiredHappiness() {
        return requiredHappiness;
    }

    public void setRequiredHappiness(double requiredHappiness) {
        this.requiredHappiness = requiredHappiness;
    }

    public List<NeedSatisfaction> getRequiredNeeds() {
        return requiredNeeds;
    }

    public void setRequiredNeeds(List<NeedSatisfaction> requiredNeeds) {
        this.requiredNeeds = requiredNeeds;
    }
}
