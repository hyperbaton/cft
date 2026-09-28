package com.hyperbaton.cft.event;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.socialclass.CensusStats;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = CftMod.MOD_ID)
public class CftLevelEvents {
    private static final int SNAPSHOT_CHECK_INTERVAL = 1200;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % SNAPSHOT_CHECK_INTERVAL == 0) {
            CensusStats.recordDailySnapshots(level);
        }
    }
}
