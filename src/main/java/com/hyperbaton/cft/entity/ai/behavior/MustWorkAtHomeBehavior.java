package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.util.JobUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

import java.util.Map;

public class MustWorkAtHomeBehavior extends Behavior<XoonglinEntity> {

    public MustWorkAtHomeBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        if (entity.getHome() == null) return false;
        BlockPos entrance = entity.getHome().getEntrance();
        if (entrance == null) return false;
        // Start when outside the home work radius
        return !JobUtil.isAtHome(entity, CftConfig.HOME_WORK_RADIUS.get());
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        BlockPos entrance = entity.getHome() != null ? entity.getHome().getEntrance() : null;
        if (entrance != null) {
            // Approach the home entrance; complete when within HOME_WORK_RADIUS
            entity.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(entrance, (float) 1.0, (int) Math.max(1, Math.floor(CftConfig.HOME_WORK_RADIUS.get())))
            );
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        // Keep running while we still must work at home and are not yet in radius
        boolean mustWork = entity.getBrain().getMemory(CftMemoryModuleType.MUST_WORK_AT_HOME.get()).isPresent();
        return mustWork && !JobUtil.isAtHome(entity, CftConfig.HOME_WORK_RADIUS.get());
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        // If we drifted away, keep resetting the walk target back to the entrance
        BlockPos entrance = entity.getHome() != null ? entity.getHome().getEntrance() : null;
        if (entrance != null && !JobUtil.isAtHome(entity, CftConfig.HOME_WORK_RADIUS.get())) {
            entity.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(entrance, (float) 1.0, (int) Math.max(1, Math.floor(CftConfig.HOME_WORK_RADIUS.get())))
            );
        } else {
            // Already in radius; let other idle behaviors run without a walk target
            entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        // Clean up the walk target when stopping (if we've satisfied the radius or memory cleared)
        entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }
}
