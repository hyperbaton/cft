package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.SocializeNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * The Xoonglin wants to spend some of its free time with another Xoonglin. Unlike the social
 * need, which only checks who lives nearby, this makes two Xoonglins actually meet. For now,
 * socializing means having a conversation (see ConverseBehavior), so the radius, duration and
 * accepted classes refer to it.
 */
public class SocializeNeed extends Need {

    public static final Codec<SocializeNeed> SOCIALIZE_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("damage").forGetter(SocializeNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(SocializeNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(SocializeNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(SocializeNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(SocializeNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(SocializeNeed::isHidden),
            Codec.BOOL.optionalFieldOf("bonus", DEFAULT_BONUS).forGetter(SocializeNeed::isBonus),
            ResourceLocation.CODEC.listOf().optionalFieldOf("classes", List.of()).forGetter(SocializeNeed::getAcceptedSocialClassIds),
            Codec.INT.optionalFieldOf("radius", 24).forGetter(SocializeNeed::getRadius),
            Codec.INT.optionalFieldOf("duration", 160).forGetter(SocializeNeed::getDuration),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(SocializeNeed::getIcon)
    ).apply(instance, SocializeNeed::new));

    private final List<ResourceLocation> acceptedSocialClassIds;
    private final int radius;
    private final int duration;

    public SocializeNeed(double damage, double damageThreshold, double providedHappiness,
                            double satisfactionThreshold, double frequency, boolean hidden, boolean bonus,
                            List<ResourceLocation> acceptedSocialClassIds, int radius, int duration,
                            Optional<ResourceLocation> icon) {
        super(damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden, bonus, icon);
        this.acceptedSocialClassIds = List.copyOf(acceptedSocialClassIds);
        this.radius = radius;
        this.duration = duration;
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.socialize").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("writable_book"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.SOCIALIZE_NEED.get();
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new SocializeNeedSatisfier(satisfaction, isSatisfied, this);
    }

    /** Social classes the Xoonglin is willing to talk to; empty means anyone. */
    public List<ResourceLocation> getAcceptedSocialClassIds() {
        return acceptedSocialClassIds;
    }

    public boolean acceptsClass(ResourceLocation socialClassId) {
        return acceptedSocialClassIds.isEmpty() || acceptedSocialClassIds.contains(socialClassId);
    }

    /** How far away the Xoonglin looks for someone to talk to. */
    public int getRadius() {
        return radius;
    }

    /** How long, in ticks, a conversation lasts once both are together. */
    public int getDuration() {
        return duration;
    }
}
