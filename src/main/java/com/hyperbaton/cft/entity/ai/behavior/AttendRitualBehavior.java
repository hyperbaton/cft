package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.ritual.Ritual;
import com.hyperbaton.cft.world.RitualsData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;

/**
 * Makes a xoonglin walk to a ritual it was summoned to (MUST_ATTEND_RITUAL memory,
 * holding the ritual center), stand within the ritual radius and look at the
 * officiant until the ritual finishes or is cancelled.
 */
public class AttendRitualBehavior extends Behavior<XoonglinEntity> {

    private static final int REPATH_INTERVAL = 40;

    private int repathTimer;

    public AttendRitualBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 6000);
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        repathTimer = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_ATTEND_RITUAL.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        Optional<BlockPos> maybeCenter = entity.getBrain()
                .getMemory(CftMemoryModuleType.MUST_ATTEND_RITUAL.get());
        if (maybeCenter.isEmpty()) return;
        BlockPos center = maybeCenter.get();

        RitualsData data = level.getDataStorage().computeIfAbsent(RitualsData.factory(), "ritualsData");
        Optional<Ritual> maybeRitual = data.findByCenter(center);
        if (maybeRitual.isEmpty()) {
            // Ritual finished or was cancelled
            leave(entity);
            return;
        }
        Ritual ritual = maybeRitual.get();

        Entity officiant = level.getEntity(ritual.getOfficiantId());
        if (officiant == null || !officiant.isAlive()) {
            // Orphaned ritual (officiant died or despawned); clean it up
            data.removeRitual(ritual);
            leave(entity);
            return;
        }

        // Arriving after the ritual started cannot satisfy a presence need
        if (ritual.getState() == Ritual.State.IN_PROGRESS && !ritual.isAttendee(entity.getUUID())) {
            leave(entity);
            return;
        }

        double distance = entity.position().distanceTo(Vec3.atCenterOf(center));
        if (distance > Math.max(ritual.getRadius() - 1, 1)) {
            if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
                repathTimer = 0;
                entity.getNavigation().moveTo(
                        center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 1.0);
            }
            return;
        }

        entity.getNavigation().stop();
        // BlockPosTracker instead of EntityTracker: EntityTracker.isVisibleBy fetches
        // the visible_mobs memory, which xoonglin brains do not register (no sensor
        // populates it), crashing the brain tick. Refreshed every tick, so the
        // attendees still follow the officiant as it moves.
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,
                new BlockPosTracker(officiant.getEyePosition()));
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
    }

    private void leave(XoonglinEntity entity) {
        entity.getBrain().eraseMemory(CftMemoryModuleType.MUST_ATTEND_RITUAL.get());
        entity.getNavigation().stop();
    }
}
