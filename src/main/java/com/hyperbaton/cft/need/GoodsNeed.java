package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.ConsumeItemNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.crafting.Ingredient;

import static com.hyperbaton.cft.need.codec.CftCodec.INGREDIENT_CODEC;

public class GoodsNeed extends Need {

    public static final Codec<GoodsNeed> GOODS_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(GoodsNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(GoodsNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(GoodsNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(GoodsNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(GoodsNeed::getSatisfactionThreshold),
            INGREDIENT_CODEC.fieldOf("item").forGetter(GoodsNeed::getIngredient),
            Codec.DOUBLE.fieldOf("frequency").forGetter(GoodsNeed::getFrequency),
            Codec.INT.fieldOf("quantity").forGetter(GoodsNeed::getQuantity)
    ).apply(instance, GoodsNeed::new));
    private static final String GOODS_NEED_TYPE = "cft:goods_need";

    private Ingredient item;
    /**
     * How many items need to be consumed every frequency days for this to be satisfied
     */
    private int quantity;

    public static final String TAG_ITEM = "item";
    public static final String TAG_QUANTITY = "quantity";

    public GoodsNeed(String id, double damage, double damageThreshold, double providedHappiness,
                     double satisfactionThreshold, Ingredient item, double frequency, int quantity) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency);
        this.quantity = quantity;
        this.item = item;
    }

    public Ingredient getIngredient() {
        return this.item;
    }

    public Ingredient getItem() {
        return item;
    }

    public void setItem(Ingredient item) {
        this.item = item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.GOODS_NEED.get();
    }

    @Override
    public NeedSatisfier<GoodsNeed> createSatisfier() {
        return this.createSatisfier(this.getSatisfactionThreshold(), false);
    }

    @Override
    public NeedSatisfier<GoodsNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new ConsumeItemNeedSatisfier(satisfaction, isSatisfied, this);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();
        tag.put(TAG_ITEM, INGREDIENT_CODEC.encodeStart(NbtOps.INSTANCE, item)
                .result()
                .orElseThrow());
        tag.putDouble(TAG_FREQUENCY, getFrequency());
        tag.putInt(TAG_QUANTITY, quantity);
        return tag;
    }

}
