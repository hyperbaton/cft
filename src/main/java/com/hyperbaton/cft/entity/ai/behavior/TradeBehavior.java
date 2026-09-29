package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.TraderJob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;


/**
 * Walks a trader to its base (assigned structure if configured, otherwise home) and
 * stays there for the workday. All restocking/depositing happens synchronously inside
 * TraderJob.tick() once the trader is at base — this behavior's only job is travel.
 */
public class TradeBehavior extends JobBehavior<TraderJob> {

    private static final int REPATH_INTERVAL = 40;

    private int repathTimer;

    public TradeBehavior() {
        super(CftMemoryModuleType.MUST_TRADE.get(), TraderJob.class, 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return getJob(entity) != null && basePos(entity) != null && !isAtBase(entity);
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        repathTimer = 0;
        navigateToBase(entity);
    }

    @Override
    protected boolean canKeepWorking(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return getJob(entity) != null && basePos(entity) != null && !isAtBase(entity);
    }

    @Override
    protected void tickWork(ServerLevel level, XoonglinEntity entity, long gameTime) {
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateToBase(entity);
        }
    }

    @Override
    protected void stopWork(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
    }

    private boolean isAtBase(XoonglinEntity entity) {
        BlockPos base = basePos(entity);
        return base != null && base.closerToCenterThan(entity.position(), com.hyperbaton.cft.CftConfig.HOME_WORK_RADIUS.get());
    }

    private BlockPos basePos(XoonglinEntity entity) {
        TraderJob job = getJob(entity);
        if (job == null) return null;
        if (job.getRequiredStructureType() != null) {
            return entity.getAssignedStructurePos(job.getRequiredStructureType());
        }
        return entity.getHome() != null ? entity.getHome().getEntrance() : null;
    }

    private void navigateToBase(XoonglinEntity entity) {
        BlockPos base = basePos(entity);
        if (base != null) {
            entity.getNavigation().moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 1.0);
        }
    }

}
