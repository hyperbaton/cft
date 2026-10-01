package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.HearingNeed;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

public class HearingNeedSatisfier extends NeedSatisfier<HearingNeed> implements HearingSatisfier {

    /** Game times when the latest matching sounds were heard, oldest first. Not saved: after a reload it has heard nothing. */
    private final Deque<Long> heardTimes = new ArrayDeque<>();

    public HearingNeedSatisfier(double satisfaction, boolean isSatisfied, HearingNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public void onSoundHeard(XoonglinEntity xoonglin, Holder<SoundEvent> sound, Vec3 position) {
        if (getNeed().getSounds().contains(sound)) {
            hear(xoonglin.level().getGameTime());
        }
    }

    @Override
    public void onGameEventHeard(XoonglinEntity xoonglin, Holder<GameEvent> gameEvent, Vec3 position,
                                 GameEvent.Context context) {
        if (getNeed().getGameEvents().contains(gameEvent)) {
            hear(xoonglin.level().getGameTime());
        }
    }

    private void hear(long gameTime) {
        // Only the latest ones matter, since they are the last to fall out of the window
        if (heardTimes.size() >= getNeed().getEventsToRemember()) {
            heardTimes.removeFirst();
        }
        heardTimes.addLast(gameTime);
    }

    private int countHeardWithinWindow(long gameTime) {
        long windowStart = gameTime - getNeed().getWindow() * 20L;
        while (!heardTimes.isEmpty() && heardTimes.peekFirst() < windowStart) {
            heardTimes.removeFirst();
        }
        return heardTimes.size();
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (mob.level().isClientSide) {
            return isSatisfied();
        }

        if (getNeed().isSatisfiedBy(countHeardWithinWindow(mob.level().getGameTime()))) {
            super.satisfy(mob);
            return true;
        }

        return failAndSeek(mob);
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
    }
}
