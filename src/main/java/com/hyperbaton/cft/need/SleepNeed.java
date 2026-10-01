package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.SleepNeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The Xoonglin needs to sleep in a bed inside its home. It goes to bed during the rest time of
 * its schedule or, if it has no schedule, at night.
 */
public class SleepNeed extends Need {

    public static final Codec<SleepNeed> SLEEP_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec()
    ).apply(instance, SleepNeed::new));

    public SleepNeed(Properties properties) {
        super(properties);
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.sleep").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("red_bed"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.SLEEP_NEED.get();
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new SleepNeedSatisfier(satisfaction, isSatisfied, this);
    }
}
