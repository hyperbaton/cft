package com.hyperbaton.cft.entity.spawner;

import com.google.common.collect.Lists;
import com.hyperbaton.cft.CftMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = CftMod.MOD_ID)
public class SpawnHandler {

    public static List<CustomSpawner> specialSpawners = Lists.newArrayList(new XunguiSpawner());

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.level instanceof ServerLevel serverLevel){
            boolean doMobSpawning = serverLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
            if (doMobSpawning) {
                for (CustomSpawner specialSpawner : specialSpawners) {
                    specialSpawner.tick(serverLevel, serverLevel.getDifficulty() != Difficulty.PEACEFUL, true);
                }
            }
        }
    }
}
