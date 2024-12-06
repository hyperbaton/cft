package com.hyperbaton.cft.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class FindPotentialMatesSensor extends Sensor<XoonglinEntity> {
    private static final double MAXIMUM_MATING_RANGE = 100.0;

    @Override
    protected void doTick(ServerLevel serverLevel, XoonglinEntity xoonglin) {
        findPotentialMates(serverLevel, xoonglin, MAXIMUM_MATING_RANGE)
                .stream()
                .min(Comparator.comparingDouble(candidate -> candidate.getEyePosition().distanceTo(xoonglin.getEyePosition())))
                .ifPresentOrElse(potentialMate -> xoonglin.getBrain()
                        .setMemory(CftMemoryModuleType.MATING_CANDIDATE.get(), potentialMate.getStringUUID()),
                        () -> xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MATING_CANDIDATE.get()));
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(CftMemoryModuleType.CAN_MATE.get(), CftMemoryModuleType.MATING_CANDIDATE.get());
    }

    private List<XoonglinEntity> findPotentialMates(ServerLevel serverLevel, XoonglinEntity xoonglin, double radius) {
        return serverLevel.getEntitiesOfClass(
                XoonglinEntity.class,
                xoonglin.getBoundingBox().inflate(radius),
                mate -> mate.canMate() &&
                        !mate.equals(xoonglin) &&// Exclude self
                        mate.getSocialClass().equals(xoonglin.getSocialClass())
        );
    }

}
