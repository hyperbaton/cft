package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.PetNeed;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        Set<EntityType<?>> acceptedTypes = need.getEntityTypes().stream()
                .map(BuiltInRegistries.ENTITY_TYPE::get)
                .collect(Collectors.toSet());

        AABB area = new AABB(mob.blockPosition()).inflate(radius);
        List<Entity> nearby = mob.level().getEntities(
                mob,
                area,
                e -> e.isAlive() && acceptedTypes.contains(e.getType())
        );

        long matching = nearby.size();

        if (matching >= min && matching <= max) {
            super.satisfy(mob);
            return true;
        }

        this.unsatisfy(need.getFrequency(), mob);
        mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
        addMemoriesForSatisfaction(mob);
        return false;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
    }
}
