package com.hyperbaton.cft.entity.spawner;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;

import com.hyperbaton.cft.need.NeedUtils;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.home.HouseStructure;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class XoonglinSpawner implements CustomSpawner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private int nextTick;

    @Override
    public int tick(ServerLevel serverLevel, boolean b, boolean b1) {
        RandomSource randomSource = serverLevel.random;
        --this.nextTick;
        if (this.nextTick > 0) {
            return 0;
        } else {
            this.nextTick += (60 + randomSource.nextInt(60) * 20);

            StructuresData structuresData = serverLevel.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
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
                                structuresData.getStructures().stream()
                                        .filter(Structure::hasCapacity)
                                        .filter(s -> s.getLeaderId().equals(player.getUUID()))
                                        .filter(s -> houseMeetsNeed(s, socialClass))
                                        .map(s -> new AbstractMap.SimpleEntry<>(socialClass, s)))
                        .findAny()
                        .map(pair -> spawnXoonglin(serverLevel, pair.getValue(), pair.getKey(), player.getUUID()))
                        .ifPresent(didSpawn -> {
                            if (didSpawn) {
                                structuresData.setDirty();
                            }
                        });
            }
        }
        return 1;
    }

    private boolean houseMeetsNeed(Structure structure, SocialClass socialClass) {
        return NeedUtils.classMeetsStructureType(socialClass, structure.getStructureTypeId());
    }

    private boolean spawnXoonglin(ServerLevel serverLevel, Structure house, SocialClass socialClass, UUID leaderId) {
        XoonglinEntity xoonglin = CftEntities.XOONGLIN.get().spawn(serverLevel, house.getKeyBlockPos(), MobSpawnType.TRIGGERED);
        if (xoonglin != null) {
            updateSpawnedXoonglin(xoonglin, house, socialClass, leaderId);
            return true;
        }
        return false;
    }

    public static void updateSpawnedXoonglin(XoonglinEntity xoonglin, Structure house, SocialClass socialClass, UUID leaderId) {
        house.addUser(xoonglin.getUUID());
        xoonglin.setLeaderId(leaderId);
        xoonglin.setHome(HouseStructure.of(house));
        xoonglin.setSocialClass(socialClass);
        xoonglin.setNeeds(NeedUtils.getNeedsForClass(xoonglin.getSocialClass()));
        xoonglin.getEntityData().set(XoonglinEntity.SOCIAL_CLASS_NAME, xoonglin.getSocialClass().getId());
        xoonglin.setJob(socialClass.getRandomJob(xoonglin.getRandom(), xoonglin.isBaby()));
        xoonglin.applyClassMaxHealth();
        LOGGER.trace("Xoonglin spawned");
        LOGGER.trace("Home house {} with owner and leaderId: {}", house.getStructureTypeId(), house.getLeaderId());
    }
}
