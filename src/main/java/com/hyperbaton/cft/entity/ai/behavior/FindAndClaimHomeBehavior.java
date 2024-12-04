package com.hyperbaton.cft.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.capability.need.HomeNeed;
import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.structure.home.XunguiHome;
import com.hyperbaton.cft.world.HomesData;
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

public class FindAndClaimHomeBehavior extends Behavior<XunguiEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MAX_SEARCHING_TIME = 2000;
    // A timer to limit how long the xunguis spends looking for a home
    private int currentSearchingTime;

    public FindAndClaimHomeBehavior() {
        super(ImmutableMap.of(CftMemoryModuleType.HOME_NEEDED.get(), MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XunguiEntity entity) {
        LOGGER.trace("Starting behavior for finding a home");
        // Only start if the Xungui doesn't have a home
        return entity.getHome() == null;
    }

    @Override
    protected void start(ServerLevel level, XunguiEntity xungui, long gameTime) {
        HomesData homesData = level.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");

        Optional<XunguiHome> nearestHome = findNearestHome(xungui.blockPosition(), xungui.getLeaderId(), homesData, getHomeNeed(xungui.getSocialClass().getNeeds()));

        nearestHome.ifPresent(home -> {
            if (xungui.getNavigation().createPath(home.getEntrance(), 0) != null) {
                xungui.getBrain().setMemory(CftMemoryModuleType.HOME_CANDIDATE_POSITION.get(), home.getEntrance());
            } else {
                // Forget unreachable home
                homesData.getHomes().remove(home);
            }
        });

        currentSearchingTime = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XunguiEntity xungui, long gameTime) {
        return xungui.getBrain().hasMemoryValue(CftMemoryModuleType.HOME_CANDIDATE_POSITION.get()) &&
                currentSearchingTime < MAX_SEARCHING_TIME;
    }

    @Override
    protected void tick(ServerLevel level, XunguiEntity xungui, long gameTime) {
        xungui.getBrain().getMemory(CftMemoryModuleType.HOME_CANDIDATE_POSITION.get()).ifPresent(pos -> {
            if (xungui.distanceToSqr(pos.getCenter()) < 10.0D) {
                // If reached, claim the home
                HomesData homesData = level.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
                homesData.getHomes().stream()
                        .filter(home -> home.getEntrance().equals(pos))
                        .filter(home -> home.getOwnerId() == null)
                        .findFirst()
                        .ifPresent(home -> {
                            home.setOwnerId(xungui.getUUID());
                            xungui.setHome(home);
                            xungui.getBrain().setMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get(), home.getContainerPos());
                            xungui.getBrain().eraseMemory(CftMemoryModuleType.HOME_CANDIDATE_POSITION.get());
                            xungui.getBrain().setMemory(CftMemoryModuleType.HOME_NEEDED.get(), false);
                            homesData.setDirty();
                        });
            } else {
                // Continue navigation to the target home
                xungui.getNavigation().moveTo(xungui.getNavigation().createPath(pos, 1), 1);
            }
        });
        currentSearchingTime++;
    }

    @Override
    protected void stop(ServerLevel level, XunguiEntity xungui, long gameTime) {
        xungui.getBrain().eraseMemory(CftMemoryModuleType.HOME_CANDIDATE_POSITION.get());
    }

    private Optional<XunguiHome> findNearestHome(BlockPos blockPos, UUID leaderId, HomesData homesData, HomeNeed homeNeed) {
        return homesData.getHomes().stream()
                .filter(home -> home.getOwnerId() == null)  // Empty homes
                .filter(home -> home.getLeaderId().equals(leaderId))    // Owned by the leader of the mob
                .filter(home -> home.getSatisfiedNeed().equals(homeNeed.getId()))   // That satisfy their need
                .min(Comparator.comparingInt(home -> home.getEntrance().distManhattan(blockPos)));  // Nearest one
    }

    private HomeNeed getHomeNeed(List<String> needs) {
        return (HomeNeed) needs.stream()
                .map(need -> CftRegistry.NEEDS.get(new ResourceLocation(need)))
                .filter(need -> need instanceof HomeNeed)
                .findFirst().orElseThrow();
    }
}
