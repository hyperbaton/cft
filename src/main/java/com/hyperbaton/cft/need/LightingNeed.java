package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.LightingNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class LightingNeed extends Need {

    public static final Codec<LightingNeed> LIGHTING_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec(),
            Codec.INT.fieldOf("min_light").forGetter(LightingNeed::getMinLight),
            Codec.INT.optionalFieldOf("radius", 0).forGetter(LightingNeed::getRadius)
    ).apply(instance, LightingNeed::new));

    private final int minLight; // 0..15
    private final int radius;   // sampling radius; 0 = only mob position

    public LightingNeed(Properties properties, int minLight, int radius) {
        super(properties);
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
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.lighting").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("torch"));
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
