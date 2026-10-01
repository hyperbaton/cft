package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.util.RegistryEntries;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.PetNeed;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class PetNeedSatisfier extends NeedSatisfier<PetNeed> {

    public PetNeedSatisfier(double satisfaction, boolean isSatisfied, PetNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        PetNeed need = getNeed();

        int radius = Math.max(0, need.getRadius());
        int min = Math.max(0, need.getMinCount());
        int max = need.getMaxCount();
        if (max < min) {
            max = min;
        }

        AABB area = new AABB(mob.blockPosition()).inflate(radius);
        List<Entity> nearby = mob.level().getEntities(
                mob,
                area,
                e -> e.isAlive() && need.getEntityTypes().contains(e.getType().builtInRegistryHolder())
        );

        long matching = nearby.size();

        if (matching >= min && matching <= max) {
            super.satisfy(mob);
            return true;
        }

        return failAndSeek(mob);
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
    }
}
