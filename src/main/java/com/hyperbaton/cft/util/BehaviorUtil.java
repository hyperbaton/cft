package com.hyperbaton.cft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helpers shared by Xoonglin behaviors.
 */
public final class BehaviorUtil {
    private BehaviorUtil() {}

    /** Whether a Xoonglin fits standing at this position: two free blocks on top of a solid one. */
    public static boolean canStandAt(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    /**
     * Where a Xoonglin can stand among the given blocks: on them (e.g. the floor of a plaza) or
     * in them (e.g. the air inside a house).
     */
    public static List<BlockPos> standablePositions(Level level, Collection<BlockPos> blocks) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        for (BlockPos pos : blocks) {
            if (canStandAt(level, pos)) {
                positions.add(pos);
            } else if (canStandAt(level, pos.above())) {
                positions.add(pos.above());
            }
        }
        return List.copyOf(positions);
    }
}
