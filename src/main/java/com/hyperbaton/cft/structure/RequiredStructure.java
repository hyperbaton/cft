package com.hyperbaton.cft.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A requirement of a compound structure: a number of already detected structures of a
 * given type that must exist within a distance of the compound's surface.
 */
public record RequiredStructure(String structureType, int min, int max, int maxDistance) {

    public static final Codec<RequiredStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("structure_type").forGetter(RequiredStructure::structureType),
            Codec.INT.optionalFieldOf("min", 1).forGetter(RequiredStructure::min),
            Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(RequiredStructure::max),
            Codec.INT.optionalFieldOf("max_distance", 16).forGetter(RequiredStructure::maxDistance)
    ).apply(instance, RequiredStructure::new));
}
