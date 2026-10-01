package com.hyperbaton.cft.structure;

import com.hyperbaton.cft.util.RegistryEntries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;


public class ValidBlock {
    private static final int ZERO_QUANTITY = 0;
    private static final int INFINITE_QUANTITY = 99999;
    private static final double ZERO_PERCENTAGE = 0.0;
    private static final double TOP_PERCENTAGE = 100.0;

    public static final Codec<ValidBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryEntries.codec(Registries.BLOCK).fieldOf("block").forGetter(ValidBlock::getBlock),
            Codec.INT.orElse(ZERO_QUANTITY).fieldOf("min_quantity").forGetter(ValidBlock::getMinQuantity),
            Codec.INT.orElse(INFINITE_QUANTITY).fieldOf("max_quantity").forGetter(ValidBlock::getMaxQuantity),
            Codec.DOUBLE.orElse(ZERO_PERCENTAGE).fieldOf("min_percentage").forGetter(ValidBlock::getMinPercentage),
            Codec.DOUBLE.orElse(TOP_PERCENTAGE).fieldOf("max_percentage").forGetter(ValidBlock::getMaxPercentage)
    ).apply(instance, ValidBlock::new));
    private RegistryEntries<Block> block;

    private int minQuantity = ZERO_QUANTITY;
    private int maxQuantity = INFINITE_QUANTITY;
    private double minPercentage = ZERO_PERCENTAGE;
    private double maxPercentage = TOP_PERCENTAGE;

    public ValidBlock(RegistryEntries<Block> block, int minQuantity, int maxQuantity, double minPercentage, double maxPercentage) {
        this.block = block;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.minPercentage = minPercentage;
        this.maxPercentage = maxPercentage;
    }

    public RegistryEntries<Block> getBlock() {
        return block;
    }

    public void setBlock(RegistryEntries<Block> block) {
        this.block = block;
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
