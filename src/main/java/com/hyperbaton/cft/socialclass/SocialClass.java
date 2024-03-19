package com.hyperbaton.cft.socialclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class SocialClass {
    public static final Codec<SocialClass> SOCIAL_CLASS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("minHappiness").forGetter(SocialClass::getMinHappiness),
            Codec.DOUBLE.fieldOf("maxHappiness").forGetter(SocialClass::getMaxHappiness),
            Codec.STRING.listOf().fieldOf("needs").forGetter(SocialClass::getNeeds),
            SocialClassUpdate.GOODS_NEED_CODEC.listOf().fieldOf("upgrades").forGetter(SocialClass::getUpgrades),
            SocialClassUpdate.GOODS_NEED_CODEC.listOf().fieldOf("downgrades").forGetter(SocialClass::getDowngrades)
    ).apply(instance, SocialClass::new));
    private double minHappiness;
    private double maxHappiness;

    // TODO: Make this a dynamic reference to a Need object
    private List<String> needs;
    private List<SocialClassUpdate> upgrades;
    private List<SocialClassUpdate> downgrades;

    public SocialClass(double minHappiness, double maxHappiness, List<String> needs, List<SocialClassUpdate> upgrades, List<SocialClassUpdate> downgrades) {
        this.minHappiness = minHappiness;
        this.maxHappiness = maxHappiness;
        this.needs = needs;
        this.upgrades = upgrades;
        this.downgrades = downgrades;
    }

    public double getMinHappiness() {
        return minHappiness;
    }

    public void setMinHappiness(double minHappiness) {
        this.minHappiness = minHappiness;
    }

    public double getMaxHappiness() {
        return maxHappiness;
    }

    public void setMaxHappiness(double maxHappiness) {
        this.maxHappiness = maxHappiness;
    }

    public List<String> getNeeds() {
        return needs;
    }

    public void setNeeds(List<String> needs) {
        this.needs = needs;
    }

    public List<SocialClassUpdate> getUpgrades() {
        return upgrades;
    }

    public void setUpgrades(List<SocialClassUpdate> upgrades) {
        this.upgrades = upgrades;
    }

    public List<SocialClassUpdate> getDowngrades() {
        return downgrades;
    }

    public void setDowngrades(List<SocialClassUpdate> downgrades) {
        this.downgrades = downgrades;
    }
}
