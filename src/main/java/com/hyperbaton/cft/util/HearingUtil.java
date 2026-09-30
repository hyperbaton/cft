package com.hyperbaton.cft.util;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.satisfaction.HearingSatisfier;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class HearingUtil {

    /**
     * Xoonglins hear a sound as far as a player would, but never further than this. Loud sounds
     * reach very far (thunder, played at volume 10000, would be heard 160000 blocks away), and
     * searching such an area for Xoonglins would stall the server. Jukebox music reaches this far.
     */
    private static final double MAX_SOUND_RANGE = 64.0;

    /**
     * An entity plays its own sounds exactly at its position, without telling which entity it is.
     * A sound this close to a Xoonglin is taken as its own.
     */
    private static final double OWN_SOUND_DISTANCE_SQR = 1.0E-4;

    /** Lets the Xoonglins within range of a sound played on the server hear it. */
    public static void hearSound(ServerLevel level, Holder<SoundEvent> sound, float volume, Vec3 position,
                                 @Nullable Entity source) {
        double range = Math.min(sound.value().getRange(volume), MAX_SOUND_RANGE);
        double rangeSqr = range * range;
        for (XoonglinEntity xoonglin : level.getEntitiesOfClass(XoonglinEntity.class,
                AABB.ofSize(position, range * 2, range * 2, range * 2))) {
            double distanceSqr = xoonglin.position().distanceToSqr(position);
            if (xoonglin == source || distanceSqr > rangeSqr || distanceSqr < OWN_SOUND_DISTANCE_SQR) continue;
            forEachHearingSatisfier(xoonglin, satisfier -> satisfier.onSoundHeard(xoonglin, sound, position));
        }
    }

    public static void forEachHearingSatisfier(XoonglinEntity xoonglin, Consumer<HearingSatisfier> action) {
        if (xoonglin.getNeeds() == null) return;
        xoonglin.getNeeds().stream()
                .filter(HearingSatisfier.class::isInstance)
                .map(HearingSatisfier.class::cast)
                .forEach(action);
    }
}
