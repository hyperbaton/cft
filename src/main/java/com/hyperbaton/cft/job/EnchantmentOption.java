package com.hyperbaton.cft.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * One entry in an enchanter's repertoire: an enchantment it can apply, and the level
 * range it picks from (clamped to the enchantment's own max level at runtime).
 */
public record EnchantmentOption(ResourceLocation enchantment, int minLevel, int maxLevel) {

    public static final Codec<EnchantmentOption> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("enchantment").forGetter(EnchantmentOption::enchantment),
            Codec.INT.optionalFieldOf("min_level", 1).forGetter(EnchantmentOption::minLevel),
            Codec.INT.optionalFieldOf("max_level", 1).forGetter(EnchantmentOption::maxLevel)
    ).apply(inst, EnchantmentOption::new));
}
