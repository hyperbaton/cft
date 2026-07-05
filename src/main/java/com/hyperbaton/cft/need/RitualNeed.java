package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.RitualNeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * A need satisfied by a ritual (identified by ritual_id, performed by an officiant job
 * with the same id) taking place within a radius of the xoonglin. If requires_presence
 * is set, the xoonglin must attend the ritual from beginning to end; otherwise it only
 * needs the ritual to complete nearby.
 */
public class RitualNeed extends Need {

    public static final Codec<RitualNeed> RITUAL_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(RitualNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(RitualNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(RitualNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(RitualNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(RitualNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(RitualNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(RitualNeed::isHidden),
            Codec.STRING.fieldOf("ritual_id").forGetter(RitualNeed::getRitualId),
            Codec.INT.optionalFieldOf("radius", 16).forGetter(RitualNeed::getRadius),
            Codec.BOOL.optionalFieldOf("requires_presence", false).forGetter(RitualNeed::isRequiresPresence),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(RitualNeed::getIcon)
    ).apply(instance, RitualNeed::new));

    private final String ritualId;
    private final int radius;
    private final boolean requiresPresence;

    public RitualNeed(String id, double damage, double damageThreshold, double providedHappiness,
                      double satisfactionThreshold, double frequency, boolean hidden,
                      String ritualId, int radius, boolean requiresPresence,
                      Optional<ResourceLocation> icon) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden, icon);
        this.ritualId = ritualId;
        this.radius = radius;
        this.requiresPresence = requiresPresence;
    }

    public String getRitualId() {
        return ritualId;
    }

    public int getRadius() {
        return radius;
    }

    public boolean isRequiresPresence() {
        return requiresPresence;
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.ritual").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("bell"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.RITUAL_NEED.get();
    }

    @Override
    public NeedSatisfier<RitualNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new RitualNeedSatisfier(satisfaction, isSatisfied, this);
    }
}
