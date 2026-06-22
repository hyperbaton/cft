package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.DecorationNeed;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class DecorationNeedSatisfier extends NeedSatisfier<DecorationNeed> {

    public DecorationNeedSatisfier(double satisfaction, boolean isSatisfied, DecorationNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        DecorationNeed need = getNeed();
        Level level = mob.level();
        BlockPos center = mob.blockPosition();
        int radius = need.getRadius();

        List<BlockPos> matchingPositions = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) continue;

                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (matchesBlock(state, need)) {
                        matchingPositions.add(pos);
                    }
                }
            }
        }

        if (matchingPositions.size() < need.getMinCount()) {
            this.unsatisfy(need.getFrequency(), mob);
            mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
            return false;
        }

        if (need.getMinSpread() > 0.0 && matchingPositions.size() >= 2) {
            double spread = computeSpread(matchingPositions, radius);
            if (spread < need.getMinSpread()) {
                this.unsatisfy(need.getFrequency(), mob);
                mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
                return false;
            }
        }

        super.satisfy(mob);
        return true;
    }

    /**
     * Computes a spread metric in [0, 1] using the root-mean-square pairwise distance
     * between all matching block positions, normalized by the radius.
     * 0 = all blocks clumped together, 1 = maximally spread within the sphere.
     */
    private double computeSpread(List<BlockPos> positions, int radius) {
        int n = positions.size();
        double totalDistSqr = 0;
        int pairs = 0;

        for (int i = 0; i < n; i++) {
            BlockPos a = positions.get(i);
            for (int j = i + 1; j < n; j++) {
                totalDistSqr += a.distSqr(positions.get(j));
                pairs++;
            }
        }

        double rmsDistance = Math.sqrt(totalDistSqr / pairs);
        return Math.min(1.0, rmsDistance / radius);
    }

    private boolean matchesBlock(BlockState state, DecorationNeed need) {
        Block block = need.getBlock();
        TagKey<Block> tag = need.getBlockTag();

        if (block != null && state.is(block)) return true;
        if (tag != null && state.is(tag)) return true;
        return false;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
    }
}
