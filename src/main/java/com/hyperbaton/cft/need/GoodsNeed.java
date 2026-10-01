package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.ConsumeItemNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;

import static com.hyperbaton.cft.need.codec.CftCodec.INGREDIENT_CODEC;

public class GoodsNeed extends Need {

    public static final Codec<GoodsNeed> GOODS_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec(),
            INGREDIENT_CODEC.fieldOf("item").forGetter(GoodsNeed::getIngredient),
            Codec.INT.fieldOf("quantity").forGetter(GoodsNeed::getQuantity),
            Codec.INT.optionalFieldOf("hoarding", 0).forGetter(GoodsNeed::getHoarding)
    ).apply(instance, GoodsNeed::new));
    private static final String GOODS_NEED_TYPE = "cft:goods_need";

    private final Ingredient item;
    private final int quantity;
    private final int hoarding;

    public GoodsNeed(Properties properties, Ingredient item, int quantity, int hoarding) {
        super(properties);
        this.quantity = quantity;
        this.hoarding = hoarding > 0 ? hoarding : quantity;
        this.item = item;
    }

    public Ingredient getIngredient() {
        return this.item;
    }

    public int getQuantity() {
        return quantity;
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
}
