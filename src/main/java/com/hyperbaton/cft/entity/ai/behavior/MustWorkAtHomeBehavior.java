package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.util.JobUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;


public class MustWorkAtHomeBehavior extends JobBehavior<Job> {

    private static final int REPATH_INTERVAL = 40;
    private int repathTimer;

    public MustWorkAtHomeBehavior() {
        super(CftMemoryModuleType.MUST_WORK_AT_HOME.get(), Job.class, 1200);
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
    protected boolean canKeepWorking(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return !JobUtil.isAtHome(entity, CftConfig.HOME_WORK_RADIUS.get());
    }

    @Override
    protected void tickWork(ServerLevel level, XoonglinEntity entity, long gameTime) {
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateHome(entity);
        }
    }

    @Override
    protected void stopWork(ServerLevel level, XoonglinEntity entity, long gameTime) {
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
