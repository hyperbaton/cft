package com.hyperbaton.cft.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.Optional;

public class FindAndClaimStructureBehavior extends Behavior<XoonglinEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_SEARCHING_TIME = 2000;

    private int currentSearchingTime;

    public FindAndClaimStructureBehavior() {
        super(ImmutableMap.of(CftMemoryModuleType.STRUCTURE_NEEDED.get(), MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return entity.getBrain().hasMemoryValue(CftMemoryModuleType.STRUCTURE_NEEDED.get());
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        xoonglin.getBrain().getMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get()).ifPresent(targetTypeId -> {
            StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");

            Optional<Structure> nearest = findNearestClaimableStructure(xoonglin, data, targetTypeId);
            nearest.ifPresent(structure -> {
                if (xoonglin.getNavigation().createPath(structure.getKeyBlockPos(), 0) != null) {
                    xoonglin.getBrain().setMemory(CftMemoryModuleType.STRUCTURE_CANDIDATE_POSITION.get(),
                            structure.getKeyBlockPos());
                }
            });
        });

        currentSearchingTime = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        return xoonglin.getBrain().hasMemoryValue(CftMemoryModuleType.STRUCTURE_CANDIDATE_POSITION.get())
                && currentSearchingTime < MAX_SEARCHING_TIME;
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        xoonglin.getBrain().getMemory(CftMemoryModuleType.STRUCTURE_CANDIDATE_POSITION.get()).ifPresent(pos -> {
            if (xoonglin.distanceToSqr(pos.getCenter()) < 10.0D) {
                xoonglin.getBrain().getMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get()).ifPresent(targetTypeId -> {
                    StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
                    data.getStructures().stream()
                            .filter(s -> s.getKeyBlockPos().equals(pos))
                            .filter(s -> s.getStructureTypeId().equals(targetTypeId))
                            .filter(Structure::hasCapacity)
                            .findFirst()
                            .ifPresent(structure -> {
                                structure.addUser(xoonglin.getUUID());
                                xoonglin.assignStructure(structure.getStructureTypeId(), structure.getKeyBlockPos());
                                xoonglin.getBrain().eraseMemory(CftMemoryModuleType.STRUCTURE_CANDIDATE_POSITION.get());
                                xoonglin.getBrain().eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
                                data.setDirty();
                                LOGGER.debug("Xoonglin {} claimed structure {} at {}",
                                        xoonglin.getName().getString(), structure.getStructureTypeId(), pos);
                            });
                });
            } else {
                xoonglin.getNavigation().moveTo(xoonglin.getNavigation().createPath(pos, 1), 1);
            }
        });
        currentSearchingTime++;
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.STRUCTURE_CANDIDATE_POSITION.get());
    }

    private Optional<Structure> findNearestClaimableStructure(XoonglinEntity xoonglin, StructuresData data, String targetTypeId) {
        BlockPos xoonglinPos = xoonglin.blockPosition();
        return data.getStructures().stream()
                .filter(s -> s.getStructureTypeId().equals(targetTypeId))
                .filter(Structure::hasCapacity)
                .filter(s -> s.getLeaderId().equals(xoonglin.getLeaderId()))
                .filter(s -> !s.isUser(xoonglin.getUUID()))
                .min(Comparator.comparingInt(s -> s.getKeyBlockPos().distManhattan(xoonglinPos)));
    }
}
