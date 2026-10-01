package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.RitualNeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A need satisfied by a ritual (identified by ritual_id, performed by an officiant job
 * with the same id) taking place within a radius of the xoonglin. If requires_presence
 * is set, the xoonglin must attend the ritual from beginning to end; otherwise it only
 * needs the ritual to complete nearby.
 */
public class RitualNeed extends Need {

    public static final Codec<RitualNeed> RITUAL_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec(),
            Codec.STRING.fieldOf("ritual_id").forGetter(RitualNeed::getRitualId),
            Codec.INT.optionalFieldOf("radius", 16).forGetter(RitualNeed::getRadius),
            Codec.BOOL.optionalFieldOf("requires_presence", false).forGetter(RitualNeed::isRequiresPresence)
    ).apply(instance, RitualNeed::new));

    private final String ritualId;
    private final int radius;
    private final boolean requiresPresence;

    public RitualNeed(Properties properties, String ritualId, int radius, boolean requiresPresence) {
        super(properties);
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
