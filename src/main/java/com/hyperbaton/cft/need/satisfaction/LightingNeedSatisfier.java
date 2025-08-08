package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.LightingNeed;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

public class LightingNeedSatisfier extends NeedSatisfier<LightingNeed> {

    public LightingNeedSatisfier(double satisfaction, boolean isSatisfied, LightingNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        LightingNeed need = getNeed();

        if (mob.level().isClientSide) {
            // Authoritative checks run on server; keep the last known state client-side
            return isSatisfied();
        }

        Level level = mob.level();
        BlockPos base = mob.blockPosition();
        int threshold = Mth.clamp(need.getMinLight(), 0, 15);
        int radius = Math.max(0, need.getRadius());

        double lightValue;

        if (radius <= 0) {
            // Single-point check (feet and head averaged to reduce false negatives)
            int feet = level.getMaxLocalRawBrightness(base);
            int head = level.getMaxLocalRawBrightness(base.above());
            lightValue = (feet + head) / 2.0;
        } else {
            // Sample a small, fixed set around the mob to approximate surroundings
            Set<BlockPos> samples = new HashSet<>();
            samples.add(base);
            samples.add(base.above());

            // Cross and diagonals on the horizontal plane, at feet and head height
            int r = radius;
            int[] offs = new int[]{-r, 0, r};
            for (int dx : offs) {
                for (int dz : offs) {
                    if (dx == 0 && dz == 0) continue;
                    samples.add(base.offset(dx, 0, dz));
                    samples.add(base.offset(dx, 1, dz));
                }
            }

            // Compute average combined light over samples
            int sum = 0;
            int count = 0;
            for (BlockPos p : samples) {
                sum += level.getMaxLocalRawBrightness(p);
                count++;
            }
            lightValue = count > 0 ? (sum / (double) count) : 0.0;
        }

        if (lightValue >= threshold) {
            super.satisfy(mob);
            return true;
        }

        // Not satisfied: decay and apply happiness penalty
        this.unsatisfy(need.getFrequency(), mob);
        mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
        addMemoriesForSatisfaction(mob);
        return false;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {

    }
}
