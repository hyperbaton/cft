package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.entity.spawner.XunguiSpawner;
import com.hyperbaton.cft.structure.home.XunguiHome;
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

public class MateBehavior extends Behavior<XunguiEntity> {

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
    protected boolean checkExtraStartConditions(ServerLevel level, XunguiEntity xungui) {
        return xungui.getBrain().getMemory(CftMemoryModuleType.CAN_MATE.get()).isPresent() &&
                xungui.getBrain().getMemory(CftMemoryModuleType.CAN_MATE.get()).get() &&
                xungui.getBrain().hasMemoryValue(CftMemoryModuleType.MATING_CANDIDATE.get());
    }

    @Override
    protected void start(ServerLevel level, XunguiEntity xungui, long gameTime) {
        getMate(level, xungui).ifPresent(mateCandidate -> xungui.getNavigation().moveTo(mateCandidate, 1.0));
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XunguiEntity xungui, long pGameTime) {
        return xungui.getBrain().hasMemoryValue(CftMemoryModuleType.CAN_MATE.get()) &&
                xungui.getBrain().getMemory(CftMemoryModuleType.CAN_MATE.get()).get() &&
                xungui.getBrain().hasMemoryValue(CftMemoryModuleType.MATING_CANDIDATE.get()) &&
                closeEnoughToMate(level, xungui);
    }

    @Override
    protected void tick(ServerLevel level, XunguiEntity xungui, long pGameTime) {
        getMate(level, xungui).ifPresent(mateCandidate -> xungui.getNavigation().moveTo(mateCandidate, 1.0));
    }

    @Override
    protected void stop(ServerLevel level, XunguiEntity xungui, long pGameTime) {
        Optional<XunguiEntity> mate = getMate(level, xungui);
        // Only mate if there is a mate and it's close enough
        if (mate.isEmpty() ||
                !(xungui.getEyePosition()
                        .distanceTo(mate.get().getEyePosition()) <= MIN_DISTANCE_FOR_MATING)) {
            return;
        }
        Optional<XunguiHome> home = findAvailableHome(level, xungui);
        // Only mate if the child will have a home to go to.
        if (home.isEmpty()) {
            return;
        }
        XunguiEntity offspring = CftEntities.XUNGUI.get().spawn(level, xungui.getOnPos(), MobSpawnType.BREEDING);
        if (offspring != null) {
            offspring.setBaby(true);
            XunguiSpawner.updateSpawnedXungui(offspring, home.get(), xungui.getSocialClass(), xungui.getLeaderId());
            level.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData").setDirty();
        }
        xungui.resetMatingDelay();
        xungui.getBrain().eraseMemory(CftMemoryModuleType.CAN_MATE.get());
        xungui.getBrain().eraseMemory(CftMemoryModuleType.MATING_CANDIDATE.get());
        mate.ifPresent(mateXungui -> {
            mateXungui.resetMatingDelay();
            mateXungui.getBrain().eraseMemory(CftMemoryModuleType.CAN_MATE.get());
            mateXungui.getBrain().eraseMemory(CftMemoryModuleType.MATING_CANDIDATE.get());
        });
    }

    private Optional<XunguiEntity> getMate(ServerLevel level, XunguiEntity xungui) {
        if (xungui.getBrain().getMemory(CftMemoryModuleType.MATING_CANDIDATE.get()).isPresent()) {
            return (Optional<XunguiEntity>) level.getEntities(CftEntities.XUNGUI.get(),
                            candidate -> candidate.getStringUUID()
                                    .equals(xungui.getBrain().getMemory(CftMemoryModuleType.MATING_CANDIDATE.get()).get()))
                    .stream().findFirst();
        } else {
            return Optional.empty();
        }
    }

    private Optional<XunguiHome> findAvailableHome(ServerLevel serverLevel, XunguiEntity xungui) {
        return serverLevel.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData")
                .getHomes().stream()
                .filter(home -> home.getLeaderId().equals(xungui.getLeaderId()) &&
                        home.getOwnerId() == null &&
                        xungui.getSocialClass().getNeeds().contains(home.getSatisfiedNeed()))
                .min(Comparator.comparingDouble(home -> Vec3.atCenterOf(home.getEntrance()).distanceToSqr(xungui.getEyePosition())));
    }

    /**
     * Whether or not the mating candidate is close enough to mate.
     * If for some reason the candidate doesn't exist, it returns false so we can end the behavior.
     */
    private boolean closeEnoughToMate(ServerLevel level, XunguiEntity xungui) {
        return getMate(level, xungui)
                .filter(xunguiEntity -> xungui.getEyePosition()
                        .distanceTo(xunguiEntity.getEyePosition()) <= MIN_DISTANCE_FOR_MATING)
                .isPresent();
    }
}