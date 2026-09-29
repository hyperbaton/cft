package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Free time behavior for visit needs: walks to the structure in MUST_VISIT and, once the need
 * is satisfied there, strolls around inside it while the VISITING memory lasts. Talking to
 * someone takes precedence, so it steps aside while the Xoonglin has a conversation partner.
 */
public class VisitStructureBehavior extends Behavior<XoonglinEntity> {
    private static final int MAX_DURATION = 6000;
    private static final double WALK_SPEED = 0.8;
    private static final double STROLL_SPEED = 0.6;
    private static final int RETARGET_INTERVAL = 40;
    private static final int MIN_STROLL_PAUSE = 60;
    private static final int MAX_STROLL_PAUSE = 160;

    @Nullable
    private BlockPos visitedKeyPos;
    private List<BlockPos> strollSpots = List.of();
    private int ticksUntilRetarget;

    public VisitStructureBehavior() {
        super(Map.of(), MAX_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity xoonglin) {
        Brain<XoonglinEntity> brain = xoonglin.getBrain();
        return (brain.hasMemoryValue(CftMemoryModuleType.MUST_VISIT.get()) || brain.hasMemoryValue(CftMemoryModuleType.VISITING.get()))
                && !brain.hasMemoryValue(CftMemoryModuleType.CONVERSATION_PARTNER.get());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        // Activity changes don't stop running behaviors, so leave as soon as IDLE isn't active
        return xoonglin.getBrain().isActive(Activity.IDLE) && checkExtraStartConditions(level, xoonglin);
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        visitedKeyPos = null;
        strollSpots = List.of();
        ticksUntilRetarget = 0;
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        if (--ticksUntilRetarget > 0 || xoonglin.getNavigation().isInProgress()) return;

        Brain<XoonglinEntity> brain = xoonglin.getBrain();
        Optional<BlockPos> visiting = brain.getMemory(CftMemoryModuleType.VISITING.get());
        if (visiting.isPresent()) {
            strollAround(level, xoonglin, visiting.get());
        } else {
            brain.getMemory(CftMemoryModuleType.MUST_VISIT.get()).ifPresent(target -> {
                xoonglin.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, WALK_SPEED);
                ticksUntilRetarget = RETARGET_INTERVAL;
            });
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        xoonglin.getNavigation().stop();
    }

    /** Wanders between spots of the visited structure, pausing a while at each one. */
    private void strollAround(ServerLevel level, XoonglinEntity xoonglin, BlockPos keyPos) {
        if (!keyPos.equals(visitedKeyPos)) {
            visitedKeyPos = keyPos;
            strollSpots = findStructure(level, keyPos)
                    .map(structure -> BehaviorUtils.standablePositions(level, structure.getAllBlockPositions()))
                    .orElse(List.of());
        }
        ticksUntilRetarget = MIN_STROLL_PAUSE + xoonglin.getRandom().nextInt(MAX_STROLL_PAUSE - MIN_STROLL_PAUSE + 1);
        if (strollSpots.isEmpty()) return;

        BlockPos spot = strollSpots.get(xoonglin.getRandom().nextInt(strollSpots.size()));
        xoonglin.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, STROLL_SPEED);
    }

    private static Optional<Structure> findStructure(ServerLevel level, BlockPos keyPos) {
        return level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData").findByKeyBlock(keyPos);
    }
}
