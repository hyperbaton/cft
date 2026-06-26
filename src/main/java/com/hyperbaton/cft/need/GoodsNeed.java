package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.ConsumeItemNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(GoodsNeed::isHidden),
            Codec.INT.fieldOf("quantity").forGetter(GoodsNeed::getQuantity),
            Codec.INT.optionalFieldOf("hoarding", 0).forGetter(GoodsNeed::getHoarding),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(GoodsNeed::getIcon)
    ).apply(instance, GoodsNeed::new));
    private static final String GOODS_NEED_TYPE = "cft:goods_need";

    private Ingredient item;
    private int quantity;
    private int hoarding;

    public static final String TAG_ITEM = "item";
    public static final String TAG_QUANTITY = "quantity";
    public static final String TAG_HOARDING = "hoarding";

    public GoodsNeed(String id, double damage, double damageThreshold, double providedHappiness,
                     double satisfactionThreshold, Ingredient item, double frequency, boolean hidden, int quantity,
                     int hoarding, Optional<ResourceLocation> icon) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden, icon);
        this.quantity = quantity;
        this.hoarding = hoarding > 0 ? hoarding : quantity;
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

    public int getHoarding() {
        return hoarding;
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.GOODS_NEED.get();
    }

    @Override
    public NeedSatisfier<GoodsNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new ConsumeItemNeedSatisfier(satisfaction, isSatisfied, this);
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.goods").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return Arrays.stream(item.getItems())
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                .toList();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();
        tag.put(TAG_ITEM, INGREDIENT_CODEC.encodeStart(NbtOps.INSTANCE, item)
                .result()
                .orElseThrow());
        tag.putDouble(TAG_FREQUENCY, getFrequency());
        tag.putInt(TAG_QUANTITY, quantity);
        tag.putInt(TAG_HOARDING, hoarding);
        return tag;
    }

}
