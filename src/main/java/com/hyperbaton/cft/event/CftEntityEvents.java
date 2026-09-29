package com.hyperbaton.cft.event;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = CftMod.MOD_ID)
public class CftEntityEvents {

    private static final int XOONGLIN_TARGET_PRIORITY = 3;

    /**
     * When enabled in the config, zombies hunt Xoonglins
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !CftConfig.MONSTERS_HUNT_XOONGLINS.get()) return;
        if (event.getEntity() instanceof Zombie zombie && !(zombie instanceof ZombifiedPiglin)) {
            zombie.targetSelector.addGoal(XOONGLIN_TARGET_PRIORITY,
                    new NearestAttackableTargetGoal<>(zombie, XoonglinEntity.class, false));
        }
    }
}
