package com.hyperbaton.cft.event;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.util.HearingUtil;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/**
 * Lets Xoonglins hear the sounds played on the server. Runs last, so it hears the sound other mods
 * may have changed, and skips the ones they cancelled.
 */
@EventBusSubscriber(modid = CftMod.MOD_ID)
public class CftSoundEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        if (event.getLevel() instanceof ServerLevel level && event.getSound() != null) {
            HearingUtil.hearSound(level, event.getSound(), event.getNewVolume(), event.getPosition(), null);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (event.getLevel() instanceof ServerLevel level && event.getSound() != null) {
            HearingUtil.hearSound(level, event.getSound(), event.getNewVolume(), event.getEntity().position(),
                    event.getEntity());
        }
    }
}
