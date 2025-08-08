package com.hyperbaton.cft.entity.spawner;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.need.NeedUtils;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.structure.home.XoonglinHome;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import oshi.util.tuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class XoonglinSpawner implements CustomSpawner {
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

                // Increment the count for each XoonglinEntity's SocialClass
                serverLevel.getEntities(CftEntities.XOONGLIN.get(), entity -> true).stream()
                        .filter(xoonglin -> xoonglin.getLeaderId() != null && xoonglin.getLeaderId().equals(player.getUUID()))
                        .map(XoonglinEntity::getSocialClass)
                        .forEach(socialClass -> socialClassCounts.merge(socialClass, 1, Integer::sum));

                // Filter out those that already reached the max spawning population
                socialClassCounts.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().getSpontaneouslySpawnPopulation() > entry.getValue())
                        .map(Map.Entry::getKey)
                        // Now find a home in which a new Xoonglin can spawn
                        .flatMap(socialClass ->
                                homesData.getHomes().stream()
                                        .filter(home ->
                                                home.getOwnerId() == null &&
                                                        home.getLeaderId().equals(player.getUUID()) &&
                                                        homeMeetsNeed(home, socialClass))
                                        .map(home -> new Pair<>(socialClass, home)))
                        .findAny()
                        // Spawn the xoonglin in the found home
                        .map(pair -> spawnXoonglin(serverLevel, pair.getB(), pair.getA(), player.getUUID()))
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
    private boolean homeMeetsNeed(XoonglinHome home, SocialClass socialClass) {
        return socialClass.getNeeds().stream().anyMatch(need -> need.equals(home.getSatisfiedNeed()));
    }

    private boolean spawnXoonglin(ServerLevel serverLevel, XoonglinHome home, SocialClass socialClass, UUID leaderId) {
        XoonglinEntity xoonglin = CftEntities.XOONGLIN.get().spawn(serverLevel, home.getEntrance(), MobSpawnType.TRIGGERED);
        if (xoonglin != null) {
            updateSpawnedXoonglin(xoonglin, home, socialClass, leaderId);
            return true;
        }
        return false;
    }

    public static void updateSpawnedXoonglin(XoonglinEntity xoonglin, XoonglinHome home, SocialClass socialClass, UUID leaderId) {
        home.setOwnerId(xoonglin.getUUID());
        xoonglin.setLeaderId(leaderId);
        xoonglin.setHome(home);
        xoonglin.getBrain().setMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get(), home.getContainerPos());
        xoonglin.setSocialClass(socialClass);
        xoonglin.setNeeds(NeedUtils.getNeedsForClass(xoonglin.getSocialClass()));
        xoonglin.getEntityData().set(XoonglinEntity.SOCIAL_CLASS_NAME, xoonglin.getSocialClass().getId());
        xoonglin.setJob(socialClass.getJob());
        System.out.println("Xoonglin created\n");
        System.out.println("Home with owner id: " + home.getOwnerId() + " and leaderId: " + home.getLeaderId() + "\n");
    }
}
