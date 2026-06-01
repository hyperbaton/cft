package com.hyperbaton.cft.entity.spawner;

import com.google.common.collect.Lists;
import com.hyperbaton.cft.CftMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

@EventBusSubscriber(modid = CftMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class SpawnHandler {

    public static List<CustomSpawner> specialSpawners = Lists.newArrayList(new XoonglinSpawner());

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel){
            boolean doMobSpawning = serverLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
            if (doMobSpawning) {
                for (CustomSpawner specialSpawner : specialSpawners) {
                    specialSpawner.tick(serverLevel, serverLevel.getDifficulty() != Difficulty.PEACEFUL, true);
                }
            }
        }
    }
}
