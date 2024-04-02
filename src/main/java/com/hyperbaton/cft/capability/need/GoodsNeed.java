package com.hyperbaton.cft.capability.need;

import com.hyperbaton.cft.CftRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public class GoodsNeed extends Need {

    public static final Codec<GoodsNeed> GOODS_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(GoodsNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(GoodsNeed::getDamage),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(GoodsNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(GoodsNeed::getSatisfactionThreshold),
            ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(GoodsNeed::getItem),
            Codec.DOUBLE.fieldOf("frequency").forGetter(GoodsNeed::getFrequency),
            Codec.INT.fieldOf("quantity").forGetter(GoodsNeed::getQuantity)
    ).apply(instance, GoodsNeed::new));
    private static final String GOODS_NEED_TYPE = "cft:goods_need";

    private Item item;
    /**
     * How many items need to be consumed every frequency days for this to be satisfied
     */
    private int quantity;

    public static final String TAG_ITEM = "item";
    public static final String TAG_QUANTITIY = "quantity";

    public GoodsNeed(String id, double damage, double providedHappiness, double satisfactionThreshold, Item item, double frequency, int quantity) {
        super(id, damage, providedHappiness, satisfactionThreshold, frequency);
        this.item = item;
        this.quantity = quantity;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public Codec<?extends Need> needType() {
        return CftRegistry.GOODS_NEED.get();
    }

    @Override
    public CompoundTag toTag(){
        CompoundTag tag = super.toTag();
        tag.put(TAG_ITEM, Optional.ofNullable(item.getShareTag(item.getDefaultInstance())).orElseThrow());
        tag.putDouble(TAG_FREQUENCY, getFrequency());
        tag.putInt(TAG_QUANTITIY, quantity);
        return tag;
    }
}
