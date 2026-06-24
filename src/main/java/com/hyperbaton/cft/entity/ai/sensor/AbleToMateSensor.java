package com.hyperbaton.cft.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.need.NeedUtils;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class AbleToMateSensor extends Sensor<XoonglinEntity> {
    @Override
    protected void doTick(@NotNull ServerLevel serverLevel, XoonglinEntity xoonglin) {
        if (xoonglin.canMate() && thereIsHomeAvailable(serverLevel, xoonglin)) {
            xoonglin.getBrain().setMemory(CftMemoryModuleType.CAN_MATE.get(), true);
        } else {
            xoonglin.getBrain().setMemory(CftMemoryModuleType.CAN_MATE.get(), false);
        }
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(CftMemoryModuleType.CAN_MATE.get());
    }

    private boolean thereIsHomeAvailable(ServerLevel serverLevel, XoonglinEntity xoonglin) {
        return serverLevel.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData")
                .getStructures().stream()
                .filter(Structure::hasCapacity)
                .filter(s -> s.getLeaderId().equals(xoonglin.getLeaderId()))
                .anyMatch(s -> NeedUtils.classMeetsStructureType(xoonglin.getSocialClass(), s.getStructureTypeId()));
    }
}
