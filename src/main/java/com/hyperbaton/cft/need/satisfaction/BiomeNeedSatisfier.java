package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.BiomeNeed;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public class BiomeNeedSatisfier extends NeedSatisfier<BiomeNeed> {
    public BiomeNeedSatisfier(double satisfaction, boolean isSatisfied, BiomeNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (!mob.level().isClientSide && isNeededBiome(mob.level().getBiome(mob.getOnPos()), need.getBiomes())) {
            super.satisfy(mob);
        } else {
            this.unsatisfy(need.getFrequency(), mob);
            mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
            addMemoriesForSatisfaction(mob);
            return false;
        }
        return true;
    }

    private boolean isNeededBiome(Holder<Biome> biome, List<String> neededBiomes) {
        return neededBiomes.stream().anyMatch(neededBiome -> biome.is(new ResourceLocation(neededBiome)));
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {

    }
}
