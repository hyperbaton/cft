package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * A need satisfier that reacts to what its Xoonglin hears, never to what the Xoonglin itself makes:
 * sounds played on the server within their range, and game events (vibrations) around it.
 * Addon needs can implement it too.
 */
public interface HearingSatisfier {

    default void onSoundHeard(XoonglinEntity xoonglin, Holder<SoundEvent> sound, Vec3 position) {
    }

    default void onGameEventHeard(XoonglinEntity xoonglin, Holder<GameEvent> gameEvent, Vec3 position,
                                  GameEvent.Context context) {
    }
}
