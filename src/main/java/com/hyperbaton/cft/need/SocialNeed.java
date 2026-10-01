package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.SocialNeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class SocialNeed extends Need {
    public static final Codec<SocialNeed> SOCIAL_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec(),
            ResourceLocation.CODEC.listOf().fieldOf("classes").forGetter(SocialNeed::getAcceptedSocialClassIds),
            Codec.INT.fieldOf("min_count").forGetter(SocialNeed::getMinCount),
            Codec.INT.fieldOf("max_count").forGetter(SocialNeed::getMaxCount),
            Codec.INT.fieldOf("radius").forGetter(SocialNeed::getRadius)
    ).apply(instance, SocialNeed::new));

    private final List<ResourceLocation> acceptedSocialClassIds;
    private final int minCount;
    private final int maxCount;
    private final int radius;

    public SocialNeed(Properties properties, List<ResourceLocation> acceptedSocialClassIds, int minCount,
                      int maxCount, int radius) {
        super(properties);
        this.acceptedSocialClassIds = List.copyOf(acceptedSocialClassIds);
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.radius = radius;
    }

    public List<ResourceLocation> getAcceptedSocialClassIds() {
        return acceptedSocialClassIds;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int getRadius() {
        return radius;
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.social").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("player_head"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.SOCIAL_NEED.get();
    }

    @Override
    public NeedSatisfier<SocialNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new SocialNeedSatisfier(satisfaction, isSatisfied, this);
    }
}
