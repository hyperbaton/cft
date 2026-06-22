package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.GuardJob;
import com.hyperbaton.cft.job.Job;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;

public class GuardBehavior extends Behavior<XoonglinEntity> {

    private static final int REPATH_INTERVAL = 40;
    private static final int SCAN_INTERVAL = 20;
    private static final double ATTACK_REACH = 2.0;
    private static final int ATTACK_COOLDOWN = 20;
    private static final int PATROL_PAUSE = 60;

    private enum State { PATROLLING, CHASING, ATTACKING, RETURNING }

    private State state;
    private LivingEntity target;
    private int repathTimer;
    private int scanTimer;
    private int attackCooldown;
    private int patrolPauseTimer;
    private BlockPos patrolTarget;

    public GuardBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return getGuardJob(entity) != null && entity.getHome() != null
                && entity.getHome().getEntrance() != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        state = State.PATROLLING;
        target = null;
        repathTimer = 0;
        scanTimer = 0;
        attackCooldown = 0;
        patrolPauseTimer = 0;
        patrolTarget = null;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_GUARD.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        GuardJob job = getGuardJob(entity);
        if (job == null) return;

        if (attackCooldown > 0) attackCooldown--;

        if (++scanTimer >= SCAN_INTERVAL) {
            scanTimer = 0;
            LivingEntity found = scanForHostile(level, entity, job);
            if (found != null && (state == State.PATROLLING || state == State.RETURNING)) {
                target = found;
                entity.setTarget(target);
                state = State.CHASING;
                repathTimer = 0;
            }
        }

        switch (state) {
            case PATROLLING -> tickPatrolling(level, entity, job);
            case CHASING -> tickChasing(entity);
            case ATTACKING -> tickAttacking(entity);
            case RETURNING -> tickReturning(entity, job);
        }
    }

    private void tickPatrolling(ServerLevel level, XoonglinEntity entity, GuardJob job) {
        if (patrolPauseTimer > 0) {
            patrolPauseTimer--;
            return;
        }

        if (patrolTarget == null || entity.getNavigation().isDone()) {
            patrolTarget = pickPatrolPoint(entity, job);
            if (patrolTarget != null) {
                navigateTo(entity, patrolTarget);
                patrolPauseTimer = PATROL_PAUSE;
            }
        }

        if (++repathTimer >= REPATH_INTERVAL) {
            repathTimer = 0;
            if (patrolTarget != null && !entity.getNavigation().isDone()) {
                navigateTo(entity, patrolTarget);
            }
        }
    }

    private void tickChasing(XoonglinEntity entity) {
        if (target == null || !target.isAlive()) {
            clearTarget(entity);
            state = State.RETURNING;
            repathTimer = 0;
            return;
        }

        double dist = entity.distanceTo(target);
        if (dist <= ATTACK_REACH) {
            entity.getNavigation().stop();
            state = State.ATTACKING;
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            entity.getNavigation().moveTo(target, 1.2);
        }
    }

    private void tickAttacking(XoonglinEntity entity) {
        if (target == null || !target.isAlive()) {
            clearTarget(entity);
            state = State.RETURNING;
            repathTimer = 0;
            return;
        }

        double dist = entity.distanceTo(target);
        if (dist > ATTACK_REACH) {
            state = State.CHASING;
            repathTimer = 0;
            return;
        }

        entity.getLookControl().setLookAt(target);
        if (attackCooldown <= 0) {
            entity.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            entity.doHurtTarget(target);
            attackCooldown = ATTACK_COOLDOWN;
        }
    }

    private void tickReturning(XoonglinEntity entity, GuardJob job) {
        BlockPos entrance = entity.getHome() != null ? entity.getHome().getEntrance() : null;
        if (entrance == null) return;

        if (entrance.closerToCenterThan(entity.position(), job.getPatrolRadius())) {
            state = State.PATROLLING;
            patrolTarget = null;
            patrolPauseTimer = 0;
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateHome(entity);
        }
    }

    private LivingEntity scanForHostile(ServerLevel level, XoonglinEntity entity, GuardJob job) {
        int r = job.getDetectionRadius();
        AABB area = entity.getBoundingBox().inflate(r);
        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class, area,
                m -> m.isAlive() && entity.hasLineOfSight(m));

        if (hostiles.isEmpty()) return null;

        Monster closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Monster m : hostiles) {
            double d = entity.distanceToSqr(m);
            if (d < closestDist) {
                closestDist = d;
                closest = m;
            }
        }
        return closest;
    }

    private BlockPos pickPatrolPoint(XoonglinEntity entity, GuardJob job) {
        BlockPos home = entity.getHome().getEntrance();
        int radius = job.getPatrolRadius();
        net.minecraft.util.RandomSource rng = entity.getRandom();

        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = rng.nextInt(radius * 2 + 1) - radius;
            int dz = rng.nextInt(radius * 2 + 1) - radius;
            BlockPos candidate = home.offset(dx, 0, dz);
            BlockPos ground = entity.level().getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
            if (Math.abs(ground.getY() - home.getY()) <= 5) {
                return ground;
            }
        }
        return home;
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
        clearTarget(entity);
        patrolTarget = null;
    }

    private void clearTarget(XoonglinEntity entity) {
        target = null;
        entity.setTarget(null);
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private void navigateHome(XoonglinEntity entity) {
        BlockPos entrance = entity.getHome() != null ? entity.getHome().getEntrance() : null;
        if (entrance != null) {
            entity.getNavigation().moveTo(entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5, 1.0);
        }
    }

    private GuardJob getGuardJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof GuardJob g ? g : null;
    }
}
