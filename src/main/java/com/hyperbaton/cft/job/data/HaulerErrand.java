package com.hyperbaton.cft.job.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

import static com.hyperbaton.cft.need.codec.CftCodec.INGREDIENT_CODEC;

public record HaulerErrand(ResourceLocation originStructure, ResourceLocation destinationStructure, List<HaulerItem> items) {

    public static final Codec<HaulerErrand> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("origin_structure").forGetter(HaulerErrand::originStructure),
            ResourceLocation.CODEC.fieldOf("destination_structure").forGetter(HaulerErrand::destinationStructure),
            HaulerItem.CODEC.listOf().fieldOf("items").forGetter(HaulerErrand::items)
    ).apply(inst, HaulerErrand::new));

    public record HaulerItem(Ingredient ingredient, int quantity) {
        public static final Codec<HaulerItem> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                INGREDIENT_CODEC.fieldOf("item").forGetter(HaulerItem::ingredient),
                Codec.INT.fieldOf("quantity").forGetter(HaulerItem::quantity)
        ).apply(inst, HaulerItem::new));
    }
}
