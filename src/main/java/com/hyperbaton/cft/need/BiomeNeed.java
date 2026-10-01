package com.hyperbaton.cft.need;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.registries.Registries;
import com.hyperbaton.cft.util.RegistryEntries;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.BiomeNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class BiomeNeed extends Need {

    public static final Codec<BiomeNeed> BIOME_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec(),
            RegistryEntries.codec(Registries.BIOME).fieldOf("biomes").forGetter(BiomeNeed::getBiomes)
    ).apply(instance, BiomeNeed::new));

    private final RegistryEntries<Biome> biomes;

    public BiomeNeed(Properties properties, RegistryEntries<Biome> biomes) {
        super(properties);
        this.biomes = biomes;
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.biome").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("grass_block"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.BIOME_NEED.get();
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new BiomeNeedSatisfier(satisfaction, isSatisfied, this);
    }

    public RegistryEntries<Biome> getBiomes() {
        return biomes;
    }
}
