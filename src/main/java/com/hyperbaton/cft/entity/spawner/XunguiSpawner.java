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

import java.util.List;
import java.util.Objects;

public class XunguiSpawner implements CustomSpawner {
    private int nextTick;
    @Override
    public int tick(ServerLevel serverLevel, boolean b, boolean b1) {
        RandomSource randomsource = serverLevel.random;
        --this.nextTick;
        if (this.nextTick > 0) {
            return 0;
        } else {
            this.nextTick += (60 + randomsource.nextInt(60) * 20);
            HomesData homesData = serverLevel.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
            for(XunguiHome home : homesData.getHomes()){
                if(home.getOwnerId() == null
                        && HomeDetection.detectHouse(home.getEntrance(), serverLevel, home.getLeaderId(),
                            getHomeNeedOfHome(home))
                        && getRandomBasicClass(serverLevel.random, getHomeNeedOfHome(home)) != null
                        /*&& randomsource.nextInt(20) == 1*/){
                    XunguiEntity xungui = CftEntities.XUNGUI.get().spawn(serverLevel, home.getEntrance(), MobSpawnType.TRIGGERED);
                    if(xungui != null){
                        home.setOwnerId(xungui.getUUID());
                        xungui.setLeaderId(home.getLeaderId());
                        xungui.setHome(home);
                        xungui.getBrain().setMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get(), home.getContainerPos());
                        xungui.setSocialClass(getRandomBasicClass(serverLevel.random, getHomeNeedOfHome(home)));
                        xungui.setNeeds(NeedUtils.getNeedsForClass(xungui.getSocialClass()));
                        xungui.getEntityData().set(XunguiEntity.SOCIAL_CLASS_NAME, xungui.getSocialClass().getId());
                        System.out.println("Xungui created\n");
                        System.out.println("Home with owner id: " + home.getOwnerId() + " and leaderId: " + home.getLeaderId() + "\n");
                    }
                }
            }
        }
        return 1;
    }

    /**
     * Given a home, return the HomeNeed that is expected to be satisfied by it.
     */
    private HomeNeed getHomeNeedOfHome(XunguiHome home) {
 w        return (HomeNeed) Objects.requireNonNull(CftRegistry.NEEDS.get(new ResourceLocation(home.getSatisfiedNeed())));
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
