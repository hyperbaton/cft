package com.hyperbaton.cft.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.crafting.Ingredient;

import static com.hyperbaton.cft.need.codec.CftCodec.INGREDIENT_CODEC;

/**
 * An ingredient with a quantity, used by jobs that consume items.
 */
public record ItemQuantity(Ingredient ingredient, int quantity) {

    public static final Codec<ItemQuantity> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            INGREDIENT_CODEC.fieldOf("item").forGetter(ItemQuantity::ingredient),
            Codec.INT.fieldOf("quantity").forGetter(ItemQuantity::quantity)
    ).apply(inst, ItemQuantity::new));
}
