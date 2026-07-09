package com.hyperbaton.cft.socialclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class SocialStructureRequirement {
    public static final Codec<SocialStructureRequirement> SOCIAL_STRUCTURE_REQUIREMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("socialClass").forGetter(SocialStructureRequirement::getSocialClass),
            Codec.DOUBLE.fieldOf("percentage").forGetter(SocialStructureRequirement::getPercentage),
            Codec.STRING.listOf().optionalFieldOf("scope", List.of()).forGetter(SocialStructureRequirement::getScope)
    ).apply(instance, SocialStructureRequirement::new));
    private String socialClass;
    private double percentage;
    /**
     * Social class IDs the percentage is computed among. If empty, the percentage is
     * computed among the whole population instead of a restricted set of classes.
     */
    private List<String> scope;

    public SocialStructureRequirement(String socialClass, double percentage, List<String> scope) {
        this.socialClass = socialClass;
        this.percentage = percentage;
        this.scope = scope;
    }

    public String getSocialClass() {
        return socialClass;
    }

    public void setSocialClass(String socialClass) {
        this.socialClass = socialClass;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }
}
