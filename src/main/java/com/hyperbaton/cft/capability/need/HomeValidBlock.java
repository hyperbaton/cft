package com.hyperbaton.cft.capability.need;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public class HomeValidBlock {
    private static final int ZERO_QUANTITY = 0;
    private static final int INFINITE_QUANTITY = 99999;
    private static final double ZERO_PERCENTAGE = 0.0;
    private static final double TOP_PERCENTAGE = 100.0;

    public static final Codec<HomeValidBlock> HOME_VALID_BLOCK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(HomeValidBlock::getItem),
            Codec.INT.orElse(ZERO_QUANTITY).fieldOf("minQuantity").forGetter(HomeValidBlock::getMinQuantity),
            Codec.INT.orElse(INFINITE_QUANTITY).fieldOf("maxQuantity").forGetter(HomeValidBlock::getMaxQuantity),
            Codec.DOUBLE.orElse(ZERO_PERCENTAGE).fieldOf("minPercentage").forGetter(HomeValidBlock::getMinPercentage),
            Codec.DOUBLE.orElse(TOP_PERCENTAGE).fieldOf("maxPercentage").forGetter(HomeValidBlock::getMaxPercentage)
    ).apply(instance, HomeValidBlock::new));
    private Item item;

    private int minQuantity = ZERO_QUANTITY;
    private int maxQuantity = INFINITE_QUANTITY;
    private double minPercentage = ZERO_PERCENTAGE;
    private double maxPercentage = TOP_PERCENTAGE;

    public HomeValidBlock(Item item, int minQuantity, int maxQuantity, double minPercentage, double maxPercentage) {
        this.item = item;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.minPercentage = minPercentage;
        this.maxPercentage = maxPercentage;
    }

    public HomeValidBlock(Item item) {
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
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
