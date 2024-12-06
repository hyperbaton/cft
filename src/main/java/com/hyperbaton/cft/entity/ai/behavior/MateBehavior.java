package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.spawner.XoonglinSpawner;
import com.hyperbaton.cft.structure.home.XoonglinHome;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class MateBehavior extends Behavior<XoonglinEntity> {

    public static final double MIN_DISTANCE_FOR_MATING = 1.2;

    public MateBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition);
    }

    public MateBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition, int pDuration) {
        super(pEntryCondition, pDuration);
    }

    public MateBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition, int pMinDuration, int pMaxDuration) {
        super(pEntryCondition, pMinDuration, pMaxDuration);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity xoonglin) {
        return xoonglin.getBrain().getMemory(CftMemoryModuleType.CAN_MATE.get()).isPresent() &&
                xoonglin.getBrain().getMemory(CftMemoryModuleType.CAN_MATE.get()).get() &&
                xoonglin.getBrain().hasMemoryValue(CftMemoryModuleType.MATING_CANDIDATE.get());
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        getMate(level, xoonglin).ifPresent(mateCandidate -> xoonglin.getNavigation().moveTo(mateCandidate, 1.0));
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity xoonglin, long pGameTime) {
        return xoonglin.getBrain().hasMemoryValue(CftMemoryModuleType.CAN_MATE.get()) &&
                xoonglin.getBrain().getMemory(CftMemoryModuleType.CAN_MATE.get()).get() &&
                xoonglin.getBrain().hasMemoryValue(CftMemoryModuleType.MATING_CANDIDATE.get()) &&
                closeEnoughToMate(level, xoonglin);
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity xoonglin, long pGameTime) {
        getMate(level, xoonglin).ifPresent(mateCandidate -> xoonglin.getNavigation().moveTo(mateCandidate, 1.0));
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity xoonglin, long pGameTime) {
        Optional<XoonglinEntity> mate = getMate(level, xoonglin);
        // Only mate if there is a mate and it's close enough
        if (mate.isEmpty() ||
                !(xoonglin.getEyePosition()
                        .distanceTo(mate.get().getEyePosition()) <= MIN_DISTANCE_FOR_MATING)) {
            return;
        }
        Optional<XoonglinHome> home = findAvailableHome(level, xoonglin);
        // Only mate if the child will have a home to go to.
        if (home.isEmpty()) {
            return;
        }
        XoonglinEntity offspring = CftEntities.XOONGLIN.get().spawn(level, xoonglin.getOnPos(), MobSpawnType.BREEDING);
        if (offspring != null) {
            offspring.setBaby(true);
            XoonglinSpawner.updateSpawnedXoonglin(offspring, home.get(), xoonglin.getSocialClass(), xoonglin.getLeaderId());
            level.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData").setDirty();
        }
        xoonglin.resetMatingDelay();
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.CAN_MATE.get());
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MATING_CANDIDATE.get());
        mate.ifPresent(mateXoonglin -> {
            mateXoonglin.resetMatingDelay();
            mateXoonglin.getBrain().eraseMemory(CftMemoryModuleType.CAN_MATE.get());
            mateXoonglin.getBrain().eraseMemory(CftMemoryModuleType.MATING_CANDIDATE.get());
        });
    }

    private Optional<XoonglinEntity> getMate(ServerLevel level, XoonglinEntity xoonglin) {
        if (xoonglin.getBrain().getMemory(CftMemoryModuleType.MATING_CANDIDATE.get()).isPresent()) {
            return (Optional<XoonglinEntity>) level.getEntities(CftEntities.XOONGLIN.get(),
                            candidate -> candidate.getStringUUID()
                                    .equals(xoonglin.getBrain().getMemory(CftMemoryModuleType.MATING_CANDIDATE.get()).get()))
                    .stream().findFirst();
        } else {
            return Optional.empty();
        }
    }

    private Optional<XoonglinHome> findAvailableHome(ServerLevel serverLevel, XoonglinEntity xoonglin) {
        return serverLevel.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData")
                .getHomes().stream()
                .filter(home -> home.getLeaderId().equals(xoonglin.getLeaderId()) &&
                        home.getOwnerId() == null &&
                        xoonglin.getSocialClass().getNeeds().contains(home.getSatisfiedNeed()))
                .min(Comparator.comparingDouble(home -> Vec3.atCenterOf(home.getEntrance()).distanceToSqr(xoonglin.getEyePosition())));
    }

    /**
     * Whether or not the mating candidate is close enough to mate.
     * If for some reason the candidate doesn't exist, it returns false so we can end the behavior.
     */
    private boolean closeEnoughToMate(ServerLevel level, XoonglinEntity xoonglin) {
        return getMate(level, xoonglin)
                .filter(xoonglinEntity -> xoonglin.getEyePosition()
                        .distanceTo(xoonglinEntity.getEyePosition()) <= MIN_DISTANCE_FOR_MATING)
                .isPresent();
    }
}