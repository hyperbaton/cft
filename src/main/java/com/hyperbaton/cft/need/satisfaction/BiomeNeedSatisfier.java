package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.util.RegistryEntries;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.BiomeNeed;


public class BiomeNeedSatisfier extends NeedSatisfier<BiomeNeed> {
    public BiomeNeedSatisfier(double satisfaction, boolean isSatisfied, BiomeNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (!mob.level().isClientSide && need.getBiomes().contains(mob.level().getBiome(mob.getOnPos()))) {
            super.satisfy(mob);
        } else {
            return failAndSeek(mob);
        }
        return true;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {

    }
}
