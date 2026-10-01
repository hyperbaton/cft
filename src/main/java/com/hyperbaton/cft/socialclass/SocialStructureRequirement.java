package com.hyperbaton.cft.socialclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class SocialStructureRequirement {
    public static final Codec<SocialStructureRequirement> SOCIAL_STRUCTURE_REQUIREMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("socialClass").forGetter(SocialStructureRequirement::getSocialClass),
            Codec.DOUBLE.fieldOf("percentage").forGetter(SocialStructureRequirement::getPercentage),
            ResourceLocation.CODEC.listOf().optionalFieldOf("scope", List.of()).forGetter(SocialStructureRequirement::getScope)
    ).apply(instance, SocialStructureRequirement::new));
    private ResourceLocation socialClass;
    private double percentage;
    /**
     * Social class IDs the percentage is computed among. If empty, the percentage is
     * computed among the whole population instead of a restricted set of classes.
     */
    private List<ResourceLocation> scope;

    public SocialStructureRequirement(ResourceLocation socialClass, double percentage, List<ResourceLocation> scope) {
        this.socialClass = socialClass;
        this.percentage = percentage;
        this.scope = scope;
    }

    public ResourceLocation getSocialClass() {
        return socialClass;
    }

    public void setSocialClass(ResourceLocation socialClass) {
        this.socialClass = socialClass;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public List<ResourceLocation> getScope() {
        return scope;
    }

    public void setScope(List<ResourceLocation> scope) {
        this.scope = scope;
    }
}
