package com.hyperbaton.cft.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record IdenticalLayerGroup(int from, int to) {

    public static final Codec<IdenticalLayerGroup> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("from").forGetter(IdenticalLayerGroup::from),
            Codec.INT.fieldOf("to").forGetter(IdenticalLayerGroup::to)
    ).apply(instance, IdenticalLayerGroup::new));
}
