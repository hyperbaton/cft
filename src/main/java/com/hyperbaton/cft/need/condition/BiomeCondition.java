package com.hyperbaton.cft.need.condition;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.util.RegistryEntries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

/** The Xoonglin stands in one of the given biomes: an id, a {@code #tag} or a list. */
public record BiomeCondition(RegistryEntries<Biome> biomes) implements NeedCondition {

    public static final Codec<BiomeCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryEntries.codec(Registries.BIOME).fieldOf("biomes").forGetter(BiomeCondition::biomes)
    ).apply(instance, BiomeCondition::new));

    @Override
    public boolean test(XoonglinEntity xoonglin) {
        return biomes.contains(xoonglin.level().getBiome(xoonglin.getOnPos()));
    }

    @Override
    public Codec<? extends NeedCondition> conditionType() {
        return CftRegistry.BIOME_CONDITION.get();
    }
}
