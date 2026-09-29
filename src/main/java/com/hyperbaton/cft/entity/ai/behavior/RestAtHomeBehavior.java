package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.structure.EnclosedBuildingBlockGroup;
import com.hyperbaton.cft.structure.home.HouseStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Runs while the REST activity is active. A Xoonglin that must sleep goes to a free bed of its
 * home and sleeps there; otherwise it just goes home and stays inside. When is it time to rest
 * or sleep is decided by the activity selection in {@link XoonglinEntity}.
 */
public class RestAtHomeBehavior extends Behavior<XoonglinEntity> {
    /** Long enough to cover a whole night, so the Xoonglin isn't woken up by a timeout. */
    private static final int MAX_DURATION = 24000;
    private static final double WALK_SPEED = 0.8;
    private static final double BED_REACH_SQR = 2.0 * 2.0;
    private static final int RETARGET_INTERVAL = 100;

    @Nullable
    private BlockPos bedPos;
    private Set<BlockPos> interior = Set.of();
    private int ticksUntilRetarget;

    public RestAtHomeBehavior() {
        super(Map.of(), MAX_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity xoonglin) {
        return xoonglin.getHome() != null;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        // Activity changes don't stop running behaviors, so leave as soon as REST isn't active
        return xoonglin.getHome() != null && xoonglin.getBrain().isActive(Activity.REST);
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        interior = interiorOf(xoonglin.getHome());
        bedPos = null;
        ticksUntilRetarget = 0;
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        if (xoonglin.isSleeping()) return;

        boolean mustSleep = xoonglin.getBrain().hasMemoryValue(CftMemoryModuleType.MUST_SLEEP.get());
        if (bedPos != null && (!mustSleep || !isFreeBed(level, bedPos))) {
            bedPos = null;
            ticksUntilRetarget = 0;
            xoonglin.getNavigation().stop();
        }

        if (bedPos != null && xoonglin.distanceToSqr(bedPos.getCenter()) <= BED_REACH_SQR) {
            xoonglin.getNavigation().stop();
            xoonglin.startSleeping(bedPos);
            return;
        }

        // Keep walking towards the current target; only pick a new one once it's reached or
        // the path failed, and not more often than the retarget interval
        if (--ticksUntilRetarget > 0 || xoonglin.getNavigation().isInProgress()) return;
        ticksUntilRetarget = RETARGET_INTERVAL;

        if (bedPos == null && mustSleep) {
            bedPos = findFreeBed(level);
        }
        if (bedPos != null) {
            walkTo(xoonglin, bedPos);
        } else if (!interior.contains(xoonglin.blockPosition())) {
            walkTo(xoonglin, findRestSpot(level, xoonglin));
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        if (xoonglin.isSleeping()) {
            xoonglin.stopSleeping();
        }
        xoonglin.getNavigation().stop();
        bedPos = null;
    }

    private static void walkTo(XoonglinEntity xoonglin, BlockPos target) {
        xoonglin.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, WALK_SPEED);
    }

    @Nullable
    private BlockPos findFreeBed(ServerLevel level) {
        return interior.stream()
                .filter(pos -> isFreeBed(level, pos))
                .findFirst()
                .orElse(null);
    }

    /** Beds are two blocks; sleeping happens on the head part, as vanilla does. */
    private static boolean isFreeBed(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BedBlock
                && state.getValue(BedBlock.PART) == BedPart.HEAD
                && !state.getValue(BedBlock.OCCUPIED);
    }

    /** A random spot inside the home where the Xoonglin can stand, or the entrance if none. */
    private BlockPos findRestSpot(ServerLevel level, XoonglinEntity xoonglin) {
        List<BlockPos> candidates = interior.stream()
                .filter(pos -> BehaviorUtils.canStandAt(level, pos))
                .toList();
        if (candidates.isEmpty()) {
            return xoonglin.getHome().getEntrance();
        }
        return candidates.get(xoonglin.getRandom().nextInt(candidates.size()));
    }

    /** Interior blocks of every storey, since multi-storey homes store one group per storey. */
    private static Set<BlockPos> interiorOf(HouseStructure home) {
        Set<BlockPos> blocks = new HashSet<>();
        home.getBlockPositions().forEach((group, positions) -> {
            if (group.startsWith(EnclosedBuildingBlockGroup.INTERIOR.getKey())) {
                blocks.addAll(positions);
            }
        });
        return blocks;
    }
}
