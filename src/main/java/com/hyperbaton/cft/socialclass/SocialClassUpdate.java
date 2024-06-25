package com.hyperbaton.cft.socialclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class SocialClassUpdate {

    public static final Codec<SocialClassUpdate> SOCIAL_CLASS_UPDATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("nextClass").forGetter(SocialClassUpdate::getNextClass),
            Codec.DOUBLE.fieldOf("requiredHappiness").forGetter(SocialClassUpdate::getRequiredHappiness),
            NeedSatisfaction.NEED_SATISFACTION_CODEC.listOf().fieldOf("requiredNeeds").forGetter(SocialClassUpdate::getRequiredNeeds),
            SocialStructureRequirement.SOCIAL_STRUCTURE_REQUIREMENT_CODEC.listOf().fieldOf("socialStructureRequirements").forGetter(SocialClassUpdate::getSocialStructureRequirements)
    ).apply(instance, SocialClassUpdate::new));
    private String nextClass;
    private double requiredHappiness;
    List<NeedSatisfaction> requiredNeeds;

    List<SocialStructureRequirement> socialStructureRequirements;

    public SocialClassUpdate(String nextClass, double requiredHappiness, List<NeedSatisfaction> requiredNeeds, List<SocialStructureRequirement> socialStructureRequirements) {
        this.nextClass = nextClass;
        this.requiredHappiness = requiredHappiness;
        this.requiredNeeds = requiredNeeds;
        this.socialStructureRequirements = socialStructureRequirements;
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

    public List<SocialStructureRequirement> getSocialStructureRequirements() {
        return socialStructureRequirements;
    }

    public void setSocialStructureRequirements(List<SocialStructureRequirement> socialStructureRequirements) {
        this.socialStructureRequirements = socialStructureRequirements;
    }
}
