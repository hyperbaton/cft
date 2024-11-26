package com.hyperbaton.cft.entity.spawner;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.capability.need.*;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.entity.memory.CftMemoryModuleType;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.structure.home.HomeDetection;
import com.hyperbaton.cft.structure.home.XunguiHome;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.CustomSpawner;

import java.util.*;

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
            // Step 1: Count homes and xunguis per leader and HomeNeed
            Map<UUID, Map<String, Long>> leaderHomeNeedCounts = countHomePerLeaderAndNeed(homesData.getHomes());
            Map<UUID, Map<String, Long>> leaderXunguiNeedCounts =
                    countXunguiPerLeaderAndNeed(serverLevel.getEntities(CftEntities.XUNGUI.get(), entity -> true));

            // Step 2: Check and spawn new Xunguis if necessary
            for (XunguiHome home : homesData.getHomes()) {
                UUID leaderId = home.getLeaderId();
                if (leaderId == null) {
                    continue; // Skip homes without a leader
                }

                HomeNeed homeNeed = getHomeNeedOfHome(home);
                if (homeNeed == null) {
                    continue; // Skip homes with invalid HomeNeed
                }

                // Check the count of Xunguis and homes for the leader and the given HomeNeed
                long homesForNeed = leaderHomeNeedCounts
                        .getOrDefault(leaderId, Collections.emptyMap())
                        .getOrDefault(homeNeed.getId(), 0L);

                long xunguisForNeed = leaderXunguiNeedCounts
                        .getOrDefault(leaderId, Collections.emptyMap())
                        .getOrDefault(homeNeed.getId(), 0L);

                // Step 3: Only spawn a new Xungui if the leader doesn't have enough Xunguis for this HomeNeed
                if (xunguisForNeed < homesForNeed
                        && HomeDetection.detectHouse(home.getEntrance(), serverLevel, leaderId, homeNeed)
                        && getRandomBasicClass(randomSource, homeNeed) != null) {

                    // Step 4: Spawn the Xungui
                    XunguiEntity xungui = CftEntities.XUNGUI.get().spawn(serverLevel, home.getEntrance(), MobSpawnType.TRIGGERED);
                    if (xungui != null) {
                        home.setOwnerId(xungui.getUUID());
                        xungui.setLeaderId(leaderId);
                        xungui.setHome(home);
                        xungui.getBrain().setMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get(), home.getContainerPos());
                        xungui.setSocialClass(getRandomBasicClass(randomSource, homeNeed));
                        xungui.setNeeds(NeedUtils.getNeedsForClass(xungui.getSocialClass()));
                        xungui.getEntityData().set(XunguiEntity.SOCIAL_CLASS_NAME, xungui.getSocialClass().getId());
                        System.out.println("Xungui created\n");
                        System.out.println("Home with owner id: " + home.getOwnerId() + " and leaderId: " + home.getLeaderId() + "\n");

                        // Update the Xungui count for the leader and HomeNeed
                        leaderXunguiNeedCounts
                                .computeIfAbsent(leaderId, k -> new HashMap<>())
                                .merge(homeNeed.getId(), 1L, Long::sum);
                    }
                }
            }
        }
        return 1;
    }

    /**
     * Count the number of xunguis that there are for each leader, classified by the home need they require
     * @param entities the xunguis in the level
     * @return A two-level map, with the counts of xunguis per need and leader
     */
    private Map<UUID, Map<String, Long>> countXunguiPerLeaderAndNeed(List<? extends XunguiEntity> entities) {
        Map<UUID, Map<String, Long>> leaderXunguiNeedCounts = new HashMap<>();
        entities.forEach(xungui -> {
            UUID leaderId = xungui.getLeaderId();
            if (leaderId == null) {
                return;
            }

            Optional<String> homeNeed = getHomeNeedOfSocialClass(xungui.getSocialClass());
            // Count the Xunguis by HomeNeed
            homeNeed.ifPresent(homeNeedId -> leaderXunguiNeedCounts
                    .computeIfAbsent(leaderId, k -> new HashMap<>())
                    .merge(homeNeedId, 1L, Long::sum));
        });
        return leaderXunguiNeedCounts;
    }

    /**
     * Count the number of homes that there are for each leader, classified by the need they satisfy
     * @param homes the list of homes in the level
     * @return A two-level map, with the counts of homes per need and leader
     */
    private Map<UUID, Map<String, Long>> countHomePerLeaderAndNeed(List<XunguiHome> homes) {
        Map<UUID, Map<String, Long>> leaderHomeNeedCounts = new HashMap<>();
        // Count homes per leader and HomeNeed
        for (XunguiHome home : homes) {
            UUID leaderId = home.getLeaderId();
            if (leaderId == null) {
                continue; // Skip homes without a leader
            }

            HomeNeed homeNeed = getHomeNeedOfHome(home);
            if (homeNeed == null) {
                continue; // Skip homes with invalid HomeNeed
            }

            leaderHomeNeedCounts
                    .computeIfAbsent(leaderId, k -> new HashMap<>())
                    .merge(homeNeed.getId(), 1L, Long::sum); // Count the homes by HomeNeed
        }
        return leaderHomeNeedCounts;
    }

    /**
     * Given a home, return the HomeNeed that is expected to be satisfied by it.
     */
    private HomeNeed getHomeNeedOfHome(XunguiHome home) {
        return (HomeNeed) Objects.requireNonNull(CftRegistry.NEEDS.get(new ResourceLocation(home.getSatisfiedNeed())));
    }

    /**
     * Given a social class, return the HomeNeed that it needs to satisfy.
     */
    private Optional<String> getHomeNeedOfSocialClass(SocialClass socialClass) {
        return socialClass.getNeeds().stream()
                .filter(need -> CftRegistry.NEEDS.get(new ResourceLocation(need)) instanceof HomeNeed)
                .findFirst();
    }

    /**
     * Get a random class that is basic (it has no downgrades) and has the given HomeNeed
     * We must filter by HomeNeed because we have to ensure the class is applicable to the home that generated the spawn
     */
    private SocialClass getRandomBasicClass(RandomSource random, HomeNeed homeNeed) {
        List<SocialClass> basicClasses = CftRegistry.SOCIAL_CLASSES.stream()
                .filter(socialClass -> socialClass.getDowngrades().isEmpty())
                .filter(socialClass -> socialClass.getNeeds().contains(homeNeed.getId()))
                .toList();
        // TODO: This is checking for every home need, even those of non basic classes. It can be optimized to skip
        //  checks for such needs
        return basicClasses.isEmpty() ? null : basicClasses.get(random.nextInt(basicClasses.size()));
    }
}
