package com.hyperbaton.cft.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record LayerRule(int from, int to, List<ValidBlock> blocks) {

    public static final Codec<LayerRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("from").forGetter(LayerRule::from),
            Codec.INT.fieldOf("to").forGetter(LayerRule::to),
            ValidBlock.CODEC.listOf().fieldOf("blocks").forGetter(LayerRule::blocks)
    ).apply(instance, LayerRule::new));

    public boolean containsLayer(int layer) {
        return layer >= from && layer <= to;
    }
}
