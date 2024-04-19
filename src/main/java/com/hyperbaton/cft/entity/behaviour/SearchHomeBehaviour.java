package com.hyperbaton.cft.entity.behaviour;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.capability.need.HomeNeed;
import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.entity.memory.CftMemoryModuleType;
import com.hyperbaton.cft.structure.home.XunguiHome;
import com.hyperbaton.cft.world.HomesData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.slf4j.Logger;

import java.util.*;

public class SearchHomeBehaviour extends Behavior<XunguiEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DELAY_TICKS = 100;
    private int delayCounter;
    public SearchHomeBehaviour(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition);
    }

    @Override
    protected void start(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {
        // Look for nearest home for the xunguis' class that's free
        LOGGER.debug("Looking for a home for mob: " + mob.getId());
        ServerLevel level = (ServerLevel) mob.level();
        HomesData homesData = level.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
        Optional<XunguiHome> opNearestHome = findNearestHome(mob.blockPosition(), mob.getLeaderId(), homesData, getHomeNeed(mob.getSocialClass().getNeeds()));
        opNearestHome.ifPresent(home -> assignHome(mob, home));
        delayCounter = 0;
    }

    private void assignHome(XunguiEntity mob, XunguiHome home) {
        mob.setHome(home);
        home.setOwnerId(mob.getUUID());
        mob.getBrain().setMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get(), home.getContainerPos());
    }

    @Override
    protected void tick(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {
        delayCounter++;
    }

    @Override
    protected void stop(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {
        super.stop(pLevel, mob, pGameTime);
    }

    @Override
    protected boolean canStillUse(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {
        return mob.getHome() == null;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel pLevel, XunguiEntity mob) {
        return mob.getHome() == null && delayCounter < DELAY_TICKS;
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
