package com.hyperbaton.cft.entity.spawner;

import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.structure.home.HomeDetection;
import com.hyperbaton.cft.structure.home.XunguiHome;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.CustomSpawner;

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
                        && new HomeDetection().detectHouse(home.getEntrance(), serverLevel, serverLevel.getPlayers(player -> player.getUUID() == home.getLeaderId()).get(0))/*&& randomsource.nextInt(20) == 1*/){
                    XunguiEntity xungui = CftEntities.XUNGUI.get().spawn(serverLevel, home.getEntrance(), MobSpawnType.TRIGGERED);
                    if(xungui != null){
                        home.setOwnerId(xungui.getUUID());
                        xungui.setLeaderId(home.getLeaderId());
                        xungui.setHome(home);
                        xungui.addHomeRelatedGoals();
                        System.out.println("Xungui created\n");
                        System.out.println("Home with owner id: " + home.getOwnerId() + " and leaderId: " + home.getLeaderId() + "\n");
                    }
                }
            }
        }
        return 1;
    }
}
