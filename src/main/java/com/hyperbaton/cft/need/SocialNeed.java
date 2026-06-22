package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.SocialNeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public class SocialNeed extends Need {
    public static final Codec<SocialNeed> SOCIAL_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(SocialNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(SocialNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(SocialNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(SocialNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(SocialNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(SocialNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(SocialNeed::isHidden),
            Codec.STRING.listOf().fieldOf("classes").forGetter(SocialNeed::getAcceptedSocialClassIds),
            Codec.INT.fieldOf("min_count").forGetter(SocialNeed::getMinCount),
            Codec.INT.fieldOf("max_count").forGetter(SocialNeed::getMaxCount),
            Codec.INT.fieldOf("radius").forGetter(SocialNeed::getRadius),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(SocialNeed::getIcon)
    ).apply(instance, SocialNeed::new));

    private final List<String> acceptedSocialClassIds;
    private final int minCount;
    private final int maxCount;
    private final int radius;

    public SocialNeed(
            String id,
            double damage,
            double damageThreshold,
            double providedHappiness,
            double satisfactionThreshold,
            double frequency,
            boolean hidden,
            List<String> acceptedSocialClassIds,
            int minCount, int maxCount,
            int radius,
            Optional<ResourceLocation> icon
    ) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden, icon);
        this.acceptedSocialClassIds = List.copyOf(acceptedSocialClassIds);
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.radius = radius;
    }

    public List<String> getAcceptedSocialClassIds() {
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
