package com.hyperbaton.cft.capability.need;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public class GoodsNeed extends Need {

    public static final Codec<GoodsNeed> GOODS_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("need_type").forGetter(GoodsNeed::getNeedType),
            Codec.DOUBLE.fieldOf("damage").forGetter(GoodsNeed::getDamage),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(GoodsNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(GoodsNeed::getSatisfactionThreshold),
            ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(GoodsNeed::getItem),
            Codec.DOUBLE.fieldOf("frequency").forGetter(GoodsNeed::getFrequency),
            Codec.INT.fieldOf("quantity").forGetter(GoodsNeed::getQuantity)
    ).apply(instance, GoodsNeed::new));

    private Item item;
    private double frequency;
    private int quantity;

    public GoodsNeed(String needType, double damage, double providedHappiness, double satisfactionThreshold, Item item, double frequency, int quantity) {
        super(needType, damage, providedHappiness, satisfactionThreshold);
        this.item = item;
        this.frequency = frequency;
        this.quantity = quantity;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
