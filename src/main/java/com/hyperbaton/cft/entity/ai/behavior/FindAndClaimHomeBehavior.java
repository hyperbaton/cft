package com.hyperbaton.cft.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.HomeNeed;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.home.HouseStructure;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FindAndClaimHomeBehavior extends Behavior<XoonglinEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MAX_SEARCHING_TIME = 2000;
    private int currentSearchingTime;

    public FindAndClaimHomeBehavior() {
        super(ImmutableMap.of(CftMemoryModuleType.HOME_NEEDED.get(), MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return entity.getHome() == null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");

        String targetStructureTypeId = getTargetStructureTypeId(xoonglin);
        Optional<Structure> nearest = findNearestAvailableHouse(xoonglin.blockPosition(), xoonglin.getLeaderId(), data, targetStructureTypeId);

        nearest.ifPresent(structure -> {
            if (xoonglin.getNavigation().createPath(structure.getKeyBlockPos(), 0) != null) {
                xoonglin.getBrain().setMemory(CftMemoryModuleType.HOME_CANDIDATE_POSITION.get(), structure.getKeyBlockPos());
            }
        });

        currentSearchingTime = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        return xoonglin.getBrain().hasMemoryValue(CftMemoryModuleType.HOME_CANDIDATE_POSITION.get()) &&
                currentSearchingTime < MAX_SEARCHING_TIME;
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        xoonglin.getBrain().getMemory(CftMemoryModuleType.HOME_CANDIDATE_POSITION.get()).ifPresent(pos -> {
            if (xoonglin.distanceToSqr(pos.getCenter()) < 10.0D) {
                StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
                data.getStructures().stream()
                        .filter(s -> s.getKeyBlockPos().equals(pos))
                        .filter(Structure::hasCapacity)
                        .findFirst()
                        .ifPresent(structure -> {
                            structure.addUser(xoonglin.getUUID());
                            xoonglin.setHome(HouseStructure.of(structure));
                            xoonglin.getBrain().setMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get(), structure.getContainerPos());
                            xoonglin.getBrain().eraseMemory(CftMemoryModuleType.HOME_CANDIDATE_POSITION.get());
                            xoonglin.getBrain().eraseMemory(CftMemoryModuleType.HOME_NEEDED.get());
                            data.setDirty();
                            LOGGER.debug("Xoonglin {} claimed home {} at {}",
                                    xoonglin.getName().getString(), structure.getStructureTypeId(), pos);
                        });
            } else {
                xoonglin.getNavigation().moveTo(xoonglin.getNavigation().createPath(pos, 1), 1);
            }
        });
        currentSearchingTime++;
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.HOME_CANDIDATE_POSITION.get());
    }

    private Optional<Structure> findNearestAvailableHouse(BlockPos blockPos, UUID leaderId, StructuresData data, String targetStructureTypeId) {
        return data.getStructures().stream()
                .filter(Structure::hasCapacity)
                .filter(s -> s.getLeaderId().equals(leaderId))
                .filter(s -> s.getStructureTypeId().equals(targetStructureTypeId))
                .min(Comparator.comparingInt(s -> s.getKeyBlockPos().distManhattan(blockPos)));
    }

    private String getTargetStructureTypeId(XoonglinEntity xoonglin) {
        HomeNeed homeNeed = (HomeNeed) xoonglin.getSocialClass().getNeeds().stream()
                .map(need -> CftRegistry.NEEDS.get(ResourceLocation.parse(need)))
                .filter(need -> need instanceof HomeNeed)
                .findFirst().orElseThrow();
        return homeNeed.getRequiredStructure();
    }
}
