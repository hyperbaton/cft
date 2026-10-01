package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.AltitudeNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class AltitudeNeed extends Need {

    public static final Codec<AltitudeNeed> ALTITUDE_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec(),
            Codec.DOUBLE.fieldOf("min_altitude").forGetter(AltitudeNeed::getMinAltitude),
            Codec.DOUBLE.fieldOf("max_altitude").forGetter(AltitudeNeed::getMaxAltitude)
    ).apply(instance, AltitudeNeed::new));

    private final double minAltitude;
    private final double maxAltitude;

    public AltitudeNeed(Properties properties, double minAltitude, double maxAltitude) {
        super(properties);
        this.minAltitude = minAltitude;
        this.maxAltitude = maxAltitude;
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.altitude").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("ladder"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.ALTITUDE_NEED.get();
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new AltitudeNeedSatisfier(satisfaction, isSatisfied, this);
    }

    public double getMinAltitude() {
        return minAltitude;
    }

    public double getMaxAltitude() {
        return maxAltitude;
    }
}
