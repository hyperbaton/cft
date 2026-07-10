package com.hyperbaton.cft.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * One effect a blesser applies: a mob effect, how long it lasts, and its amplifier.
 */
public record EffectApplication(ResourceLocation effect, int duration, int amplifier) {

    public static final Codec<EffectApplication> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(EffectApplication::effect),
            Codec.INT.fieldOf("duration").forGetter(EffectApplication::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(EffectApplication::amplifier)
    ).apply(inst, EffectApplication::new));
}
