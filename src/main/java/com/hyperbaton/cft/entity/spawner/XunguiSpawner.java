package com.hyperbaton.cft.entity.spawner;

import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.structure.home.XunguiHome;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.CustomSpawner;

public class XunguiSpawner implements CustomSpawner {
    private int nextTick;
    @Override
    public int tick(ServerLevel serverLevel, boolean b, boolean b1) {
        System.out.println("CustomSpawner hit\n");
        RandomSource randomsource = serverLevel.random;
        --this.nextTick;
        if (this.nextTick > 0) {
            return 0;
        } else {
            this.nextTick += (60 + randomsource.nextInt(60) * 20);
            HomesData homesData = serverLevel.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
            for(XunguiHome home : homesData.getHomes()){
                if(home.getOwnerId() == 0 && randomsource.nextInt(20) == 1){
                    XunguiEntity xungui = CftEntities.XUNGUI.get().create(serverLevel);
                    if(xungui != null){
                        xungui.moveTo(home.getEntrance(), 0f, 0f);
                        home.setOwnerId(xungui.getId());
                        System.out.println("Xungui created\n");
                    }
                }
            }
        }
        return 1;
    }
}
