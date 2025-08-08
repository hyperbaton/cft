package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.LightingNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class LightingNeed extends Need {

    public static final Codec<LightingNeed> LIGHTING_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(LightingNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(LightingNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(LightingNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(LightingNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(LightingNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(LightingNeed::getFrequency),
            Codec.BOOL.fieldOf("hidden").forGetter(LightingNeed::isHidden),
            Codec.INT.fieldOf("min_light").forGetter(LightingNeed::getMinLight),
            Codec.INT.optionalFieldOf("radius", 0).forGetter(LightingNeed::getRadius)
    ).apply(instance, LightingNeed::new));

    private final int minLight; // 0..15
    private final int radius;   // sampling radius; 0 = only mob position

    public LightingNeed(
            String id,
            double damage,
            double damageThreshold,
            double providedHappiness,
            double satisfactionThreshold,
            double frequency,
            boolean hidden,
            int minLight,
            int radius
    ) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden);
        this.minLight = Math.max(0, Math.min(15, minLight));
        this.radius = Math.max(0, radius);
    }

    public int getMinLight() {
        return minLight;
    }

    public int getRadius() {
        return radius;
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.LIGHTING_NEED.get();
    }

    @Override
    public NeedSatisfier<LightingNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new LightingNeedSatisfier(satisfaction, isSatisfied, this);
    }
}
