package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.job.TraderJob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

/**
 * Walks a trader to its base (assigned structure if configured, otherwise home) and
 * stays there for the workday. All restocking/depositing happens synchronously inside
 * TraderJob.tick() once the trader is at base — this behavior's only job is travel.
 */
public class TradeBehavior extends Behavior<XoonglinEntity> {

    private static final int REPATH_INTERVAL = 40;

    private int repathTimer;

    public TradeBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return getTraderJob(entity) != null && basePos(entity) != null && !isAtBase(entity);
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        repathTimer = 0;
        navigateToBase(entity);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        if (getTraderJob(entity) == null || entity.getBrain().getMemory(CftMemoryModuleType.MUST_TRADE.get()).isEmpty()) {
            return false;
        }
        return basePos(entity) != null && !isAtBase(entity);
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateToBase(entity);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
    }

    private boolean isAtBase(XoonglinEntity entity) {
        BlockPos base = basePos(entity);
        return base != null && base.closerToCenterThan(entity.position(), com.hyperbaton.cft.CftConfig.HOME_WORK_RADIUS.get());
    }

    private BlockPos basePos(XoonglinEntity entity) {
        TraderJob job = getTraderJob(entity);
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

    private TraderJob getTraderJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof TraderJob t ? t : null;
    }
}
