package com.hyperbaton.cft.entity.spawner;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.capability.need.*;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.structure.home.XunguiHome;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import oshi.util.tuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class XunguiSpawner implements CustomSpawner {
    private int nextTick;

    @Override
    public int tick(ServerLevel serverLevel, boolean b, boolean b1) {
        RandomSource randomSource = serverLevel.random;
        --this.nextTick;
        if (this.nextTick > 0) {
            return 0;
        } else {
            this.nextTick += (60 + randomSource.nextInt(60) * 20);

            HomesData homesData = serverLevel.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
            for (Player player : serverLevel.players()) {

                // Initialize map with all social classes and values set to 0
                Map<SocialClass, Integer> socialClassCounts = CftRegistry.SOCIAL_CLASSES.stream()
                        .collect(Collectors.toMap(socialClass -> socialClass, socialClass -> 0));

                // Increment the count for each XunguiEntity's SocialClass
                serverLevel.getEntities(CftEntities.XUNGUI.get(), entity -> true).stream()
                        .filter(xungui -> xungui.getLeaderId() != null && xungui.getLeaderId().equals(player.getUUID()))
                        .map(XunguiEntity::getSocialClass)
                        .forEach(socialClass -> socialClassCounts.merge(socialClass, 1, Integer::sum));

                // Filter out those that already reached the max spawning population
                socialClassCounts.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().getSpontaneouslySpawnPopulation() > entry.getValue())
                        .map(Map.Entry::getKey)
                        // Now find a home in which a new Xungui can spawn
                        .flatMap(socialClass ->
                                homesData.getHomes().stream()
                                        .filter(home ->
                                                home.getOwnerId() == null &&
                                                        home.getLeaderId().equals(player.getUUID()) &&
                                                        homeMeetsNeed(home, socialClass))
                                        .map(home -> new Pair<>(socialClass, home)))
                        .findAny()
                        // Spawn the xungui in the found home
                        .map(pair -> spawnXungui(serverLevel, pair.getB(), pair.getA(), player.getUUID()))
                        .ifPresent(didSpawn -> {
                            if (didSpawn) {
                                homesData.setDirty();
                            }
                        });
            }
        }
        return 1;
    }

    /**
     * Returns whether or not this home satisfies the home need of the given socialClass
     */
    private boolean homeMeetsNeed(XunguiHome home, SocialClass socialClass) {
        return socialClass.getNeeds().stream().anyMatch(need -> need.equals(home.getSatisfiedNeed()));
    }

    private boolean spawnXungui(ServerLevel serverLevel, XunguiHome home, SocialClass socialClass, UUID leaderId) {
        XunguiEntity xungui = CftEntities.XUNGUI.get().spawn(serverLevel, home.getEntrance(), MobSpawnType.TRIGGERED);
        if (xungui != null) {
            updateSpawnedXungui(xungui, home, socialClass, leaderId);
            return true;
        }
        return false;
    }

    public static void updateSpawnedXungui(XunguiEntity xungui, XunguiHome home, SocialClass socialClass, UUID leaderId) {
        home.setOwnerId(xungui.getUUID());
        xungui.setLeaderId(leaderId);
        xungui.setHome(home);
        xungui.getBrain().setMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get(), home.getContainerPos());
        xungui.setSocialClass(socialClass);
        xungui.setNeeds(NeedUtils.getNeedsForClass(xungui.getSocialClass()));
        xungui.getEntityData().set(XunguiEntity.SOCIAL_CLASS_NAME, xungui.getSocialClass().getId());
        System.out.println("Xungui created\n");
        System.out.println("Home with owner id: " + home.getOwnerId() + " and leaderId: " + home.getLeaderId() + "\n");
    }
}
