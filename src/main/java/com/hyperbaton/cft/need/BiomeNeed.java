package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.BiomeNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class BiomeNeed extends Need {

    public static final Codec<BiomeNeed> BIOME_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(BiomeNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(BiomeNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(BiomeNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(BiomeNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(BiomeNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(BiomeNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(BiomeNeed::isHidden),
            Codec.STRING.listOf().fieldOf("biomes").forGetter(BiomeNeed::getBiomes)
    ).apply(instance, BiomeNeed::new));

    private List<String> biomes;

    public BiomeNeed(String id, double damage, double damageThreshold, double providedHappiness,
                     double satisfactionThreshold, double frequency, boolean hidden, List<String> biomes) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden);
        this.biomes = biomes;
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.BIOME_NEED.get();
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new BiomeNeedSatisfier(satisfaction, isSatisfied, this);
    }

    public List<String> getBiomes() {
        return biomes;
    }

    public void setBiomes(List<String> biomes) {
        this.biomes = biomes;
    }
}
