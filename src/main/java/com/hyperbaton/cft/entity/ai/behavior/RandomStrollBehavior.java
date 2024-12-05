package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.entity.custom.XunguiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RandomStrollBehavior  extends Behavior<XunguiEntity> {

    public RandomStrollBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition);
    }

    @Override
    protected void start(@NotNull ServerLevel pLevel, XunguiEntity xungui, long pGameTime) {
        xungui.getNavigation().moveTo(
                xungui.getNavigation().createPath(createRandomPath(pLevel, xungui.blockPosition(), 1, 10.0),
                        1),
                0.8);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XunguiEntity xungui, long pGameTime) {
        return xungui.getNavigation().isInProgress();
    }

    public static Set<BlockPos> createRandomPath(ServerLevel level, BlockPos startPos, int steps, double maxDistance) {
        Set<BlockPos> path = new HashSet<>();
        RandomSource random = level.getRandom(); // Random source for generating offsets
        BlockPos currentPos = startPos;

        for (int i = 0; i < steps; i++) {
            // Generate random offsets within the maximum distance
            double offsetX = (random.nextDouble() - 0.5) * 2 * maxDistance;
            double offsetZ = (random.nextDouble() - 0.5) * 2 * maxDistance;

            // Floor to integer coordinates and create a new position
            BlockPos nextPos = currentPos.offset(
                    (int) offsetX,
                    0,
                    (int) offsetZ
            );

            // Check if the position is within the bounds of the world
            if (level.isInWorldBounds(nextPos)) {
                path.add(nextPos);
                currentPos = nextPos; // Move to the next position
            }
        }

        return path;
    }
}
