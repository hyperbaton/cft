package com.hyperbaton.cft.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How well lit the inside of a building must be: at least {@code minPercentage} of the spots where
 * a Xoonglin can stand must get {@code minLight} from blocks (torches, lanterns...). Daylight
 * doesn't count, so a building is lit or not regardless of the time of day, and homes, which are
 * checked again and again, aren't lost at night.
 */
public record LightingRequirement(int minLight, double minPercentage) {

    public static final Codec<LightingRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 15).fieldOf("min_light").forGetter(LightingRequirement::minLight),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("min_percentage", 1.0).forGetter(LightingRequirement::minPercentage)
    ).apply(instance, LightingRequirement::new));
}
