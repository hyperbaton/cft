package com.hyperbaton.cft.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XunguiEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class FindPotentialMatesSensor extends Sensor<XunguiEntity> {
    private static final double MAXIMUM_MATING_RANGE = 100.0;

    @Override
    protected void doTick(ServerLevel serverLevel, XunguiEntity xungui) {
        findPotentialMates(serverLevel, xungui, MAXIMUM_MATING_RANGE)
                .stream()
                .min(Comparator.comparingDouble(candidate -> candidate.getEyePosition().distanceTo(xungui.getEyePosition())))
                .ifPresent(potentialMate -> xungui.getBrain()
                        .setMemory(CftMemoryModuleType.MATING_CANDIDATE.get(), potentialMate.getStringUUID()));
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(CftMemoryModuleType.CAN_MATE.get(), CftMemoryModuleType.MATING_CANDIDATE.get());
    }

    private List<XunguiEntity> findPotentialMates(ServerLevel serverLevel, XunguiEntity xungui, double radius) {
        return serverLevel.getEntitiesOfClass(
                XunguiEntity.class,
                xungui.getBoundingBox().inflate(radius),
                mate -> mate.canMate() &&
                        !mate.equals(xungui) &&// Exclude self
                        mate.getSocialClass().equals(xungui.getSocialClass())
        );
    }

}
