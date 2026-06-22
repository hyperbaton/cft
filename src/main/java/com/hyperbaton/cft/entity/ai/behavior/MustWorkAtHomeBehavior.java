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

import java.util.Map;

public class MustWorkAtHomeBehavior extends Behavior<XoonglinEntity> {

    private static final int REPATH_INTERVAL = 40;
    private int repathTimer;

    public MustWorkAtHomeBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        if (entity.getHome() == null) return false;
        BlockPos entrance = entity.getHome().getEntrance();
        if (entrance == null) return false;
        return !JobUtil.isAtHome(entity, CftConfig.HOME_WORK_RADIUS.get());
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        repathTimer = 0;
        navigateHome(entity);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        boolean mustWork = entity.getBrain().getMemory(CftMemoryModuleType.MUST_WORK_AT_HOME.get()).isPresent();
        return mustWork && !JobUtil.isAtHome(entity, CftConfig.HOME_WORK_RADIUS.get());
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateHome(entity);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        if (JobUtil.isAtHome(entity, CftConfig.HOME_WORK_RADIUS.get())) {
            entity.getNavigation().stop();
        }
    }

    private void navigateHome(XoonglinEntity entity) {
        BlockPos entrance = entity.getHome() != null ? entity.getHome().getEntrance() : null;
        if (entrance != null) {
            entity.getNavigation().moveTo(entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5, 1.0);
        }
    }
}
