package com.hyperbaton.cft.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Block requirements for a range of storeys of a multi-storey building.
 * Each storey works like an enclosed building: floor, walls, interior and roof (ceiling).
 */
public record StoreyRule(int from, int to, List<ValidBlock> floorBlocks, List<ValidBlock> wallBlocks,
                         List<ValidBlock> interiorBlocks, List<ValidBlock> roofBlocks) {

    public static final Codec<StoreyRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("from").forGetter(StoreyRule::from),
            Codec.INT.fieldOf("to").forGetter(StoreyRule::to),
            ValidBlock.CODEC.listOf().fieldOf("floorBlocks").forGetter(StoreyRule::floorBlocks),
            ValidBlock.CODEC.listOf().fieldOf("wallBlocks").forGetter(StoreyRule::wallBlocks),
            ValidBlock.CODEC.listOf().fieldOf("interiorBlocks").forGetter(StoreyRule::interiorBlocks),
            ValidBlock.CODEC.listOf().fieldOf("roofBlocks").forGetter(StoreyRule::roofBlocks)
    ).apply(instance, StoreyRule::new));

    public boolean containsStorey(int storey) {
        return storey >= from && storey <= to;
    }
}
