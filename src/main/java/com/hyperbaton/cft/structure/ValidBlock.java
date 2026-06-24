package com.hyperbaton.cft.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Optional;

public class ValidBlock {
    private static final int ZERO_QUANTITY = 0;
    private static final int INFINITE_QUANTITY = 99999;
    private static final double ZERO_PERCENTAGE = 0.0;
    private static final double TOP_PERCENTAGE = 100.0;

    public static final Codec<ValidBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block").forGetter(validBlock -> Optional.ofNullable(validBlock.block)),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("tagBlock").forGetter(validBlock -> Optional.ofNullable(validBlock.getTagBlock())),
            Codec.INT.orElse(ZERO_QUANTITY).fieldOf("minQuantity").forGetter(ValidBlock::getMinQuantity),
            Codec.INT.orElse(INFINITE_QUANTITY).fieldOf("maxQuantity").forGetter(ValidBlock::getMaxQuantity),
            Codec.DOUBLE.orElse(ZERO_PERCENTAGE).fieldOf("minPercentage").forGetter(ValidBlock::getMinPercentage),
            Codec.DOUBLE.orElse(TOP_PERCENTAGE).fieldOf("maxPercentage").forGetter(ValidBlock::getMaxPercentage)
    ).apply(instance, ValidBlock::new));
    private Block block;
    private TagKey<Block> tagBlock;

    private int minQuantity = ZERO_QUANTITY;
    private int maxQuantity = INFINITE_QUANTITY;
    private double minPercentage = ZERO_PERCENTAGE;
    private double maxPercentage = TOP_PERCENTAGE;

    public ValidBlock(Optional<Block> block, Optional<TagKey<Block>> tagBlock, int minQuantity, int maxQuantity, double minPercentage, double maxPercentage) {
        this.block = block.orElse(null);
        this.tagBlock = tagBlock.orElse(null);
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.minPercentage = minPercentage;
        this.maxPercentage = maxPercentage;
    }

    public ValidBlock(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public TagKey<Block> getTagBlock() {
        return tagBlock;
    }

    public void setTagBlock(TagKey<Block> tagBlock) {
        this.tagBlock = tagBlock;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(int minQuantity) {
        this.minQuantity = minQuantity;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(int maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public double getMinPercentage() {
        return minPercentage;
    }

    public void setMinPercentage(double minPercentage) {
        this.minPercentage = minPercentage;
    }

    public double getMaxPercentage() {
        return maxPercentage;
    }

    public void setMaxPercentage(double maxPercentage) {
        this.maxPercentage = maxPercentage;
    }
}
