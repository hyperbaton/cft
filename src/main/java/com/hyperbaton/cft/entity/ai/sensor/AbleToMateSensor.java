package com.hyperbaton.cft.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class AbleToMateSensor extends Sensor<XunguiEntity> {
    @Override
    protected void doTick(@NotNull ServerLevel serverLevel, XunguiEntity xungui) {
        if (xungui.canMate() && thereIsHomeAvailable(serverLevel, xungui)) {
            xungui.getBrain().setMemory(CftMemoryModuleType.CAN_MATE.get(), true);
        } else {
            xungui.getBrain().setMemory(CftMemoryModuleType.CAN_MATE.get(), false);
        }
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(CftMemoryModuleType.CAN_MATE.get());
    }

    private boolean thereIsHomeAvailable(ServerLevel serverLevel, XunguiEntity xungui) {
        return serverLevel.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData")
                .getHomes().stream()
                .anyMatch(home -> home.getLeaderId().equals(xungui.getLeaderId()) &&
                        home.getOwnerId() == null &&
                        xungui.getSocialClass().getNeeds().contains(home.getSatisfiedNeed()));
    }
}
