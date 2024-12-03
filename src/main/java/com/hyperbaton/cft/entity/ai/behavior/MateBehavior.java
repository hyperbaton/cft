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
                xungui.getBrain().hasMemoryValue(CftMemoryModuleType.MATING_CANDIDATE.get()) &&
                xungui.getEyePosition().distanceTo(getMate(level, xungui).get().getEyePosition()) <= 1.0;
    }

    @Override
    protected void tick(ServerLevel level, XunguiEntity xungui, long pGameTime) {
        getMate(level, xungui).ifPresent(mateCandidate -> xungui.getNavigation().moveTo(mateCandidate, 1.0));                
    }

    @Override
    protected void stop(ServerLevel level, XunguiEntity xungui, long pGameTime) {
        Optional<XunguiEntity> mate = getMate(level, xungui);
        if (mate.isEmpty()) {
            return;
        }
        XunguiEntity offspring = CftEntities.XUNGUI.get().spawn(level, xungui.getOnPos(), MobSpawnType.BREEDING);
        Optional<XunguiHome> home = findAvailableHome(level, xungui);
        if (offspring != null && home.isPresent()) {
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
}