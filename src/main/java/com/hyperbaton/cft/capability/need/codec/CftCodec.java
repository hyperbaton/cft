package com.hyperbaton.cft.capability.need.codec;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.crafting.Ingredient;

public class CftCodec {

    /*public static final Codec<Ingredient> INGREDIENT_CODEC = Codec.PASSTHROUGH.xmap(
            json -> Ingredient.fromJson((JsonElement) json.getValue()), // Deserialize
            ingredient -> new Dynamic<>(JsonOps.INSTANCE, ingredient.toJson()) // Serialize
    );*/
    public static final Codec<Ingredient> INGREDIENT_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> Ingredient.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue()),  // Deserialize
            ingredient -> new Dynamic<>(JsonOps.INSTANCE, ingredient.toJson())       // Serialize
    );


}
