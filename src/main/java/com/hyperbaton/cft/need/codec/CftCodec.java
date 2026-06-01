package com.hyperbaton.cft.need.codec;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.crafting.Ingredient;

public class CftCodec {

    public static final Codec<Ingredient> INGREDIENT_CODEC = Ingredient.CODEC;

}
