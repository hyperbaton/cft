package com.hyperbaton.cft.capability.need;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public class HomeValidBlock {
    private static final int ZERO_QUANTITY = 0;
    private static final int INFINITE_QUANTITY = 99999;
    private static final double ZERO_PERCENTAGE = 0.0;
    private static final double TOP_PERCENTAGE = 100.0;

    public static final Codec<HomeValidBlock> HOME_VALID_BLOCK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ForgeRegistries.BLOCKS.getCodec().fieldOf("item").forGetter(HomeValidBlock::getBlock),
            Codec.INT.orElse(ZERO_QUANTITY).fieldOf("minQuantity").forGetter(HomeValidBlock::getMinQuantity),
            Codec.INT.orElse(INFINITE_QUANTITY).fieldOf("maxQuantity").forGetter(HomeValidBlock::getMaxQuantity),
            Codec.DOUBLE.orElse(ZERO_PERCENTAGE).fieldOf("minPercentage").forGetter(HomeValidBlock::getMinPercentage),
            Codec.DOUBLE.orElse(TOP_PERCENTAGE).fieldOf("maxPercentage").forGetter(HomeValidBlock::getMaxPercentage)
    ).apply(instance, HomeValidBlock::new));
    private Block block;

    private int minQuantity = ZERO_QUANTITY;
    private int maxQuantity = INFINITE_QUANTITY;
    private double minPercentage = ZERO_PERCENTAGE;
    private double maxPercentage = TOP_PERCENTAGE;

    public HomeValidBlock(Block block, int minQuantity, int maxQuantity, double minPercentage, double maxPercentage) {
        this.block = block;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.minPercentage = minPercentage;
        this.maxPercentage = maxPercentage;
    }

    public HomeValidBlock(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block item) {
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
