package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.AltitudeNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class AltitudeNeed extends Need {

    public static final Codec<AltitudeNeed> ALTITUDE_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(AltitudeNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(AltitudeNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(AltitudeNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(AltitudeNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(AltitudeNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(AltitudeNeed::getFrequency),
            Codec.DOUBLE.fieldOf("min_altitude").forGetter(AltitudeNeed::getMinAltitude),
            Codec.DOUBLE.fieldOf("max_altitude").forGetter(AltitudeNeed::getMaxAltitude)
    ).apply(instance, AltitudeNeed::new));

    private double minAltitude;
    private double maxAltitude;

    public AltitudeNeed(String id, double damage, double damageThreshold, double providedHappiness,
                        double satisfactionThreshold, double frequency, double minAltitude, double maxAltitude) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency);
        this.minAltitude = minAltitude;
        this.maxAltitude = maxAltitude;
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.ALTITUDE_NEED.get();
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier() {
        return createSatisfier(this.getSatisfactionThreshold(), false);
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new AltitudeNeedSatisfier(satisfaction, isSatisfied, this);
    }

    public double getMinAltitude() {
        return minAltitude;
    }

    public void setMinAltitude(double minAltitude) {
        this.minAltitude = minAltitude;
    }

    public double getMaxAltitude() {
        return maxAltitude;
    }

    public void setMaxAltitude(double maxAltitude) {
        this.maxAltitude = maxAltitude;
    }
}
