package com.hyperbaton.cft.socialclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SocialStructureRequirement {
    public static final Codec<SocialStructureRequirement> SOCIAL_STRUCTURE_REQUIREMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("socialClass").forGetter(SocialStructureRequirement::getSocialClass),
            Codec.DOUBLE.fieldOf("percentage").forGetter(SocialStructureRequirement::getPercentage)
    ).apply(instance, SocialStructureRequirement::new));
    private String socialClass;
    private double percentage;

    public SocialStructureRequirement(String socialClass, double percentage) {
        this.socialClass = socialClass;
        this.percentage = percentage;
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
}
