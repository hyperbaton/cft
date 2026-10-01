package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.SleepNeed;

/**
 * Satisfied by sleeping. While unsatisfied, the MUST_SLEEP memory sends the Xoonglin to bed
 * when it's time to sleep; the memory is removed once it has slept.
 */
public class SleepNeedSatisfier extends NeedSatisfier<SleepNeed> {

    public SleepNeedSatisfier(double satisfaction, boolean isSatisfied, SleepNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (mob.isSleeping()) {
            super.satisfy(mob);
            mob.getBrain().eraseMemory(CftMemoryModuleType.MUST_SLEEP.get());
            return true;
        }
        return failAndSeek(mob);
    }

    /**
     * Unlike other needs, which are refilled at the moment they drop below the threshold, this
     * one doesn't wear off while the Xoonglin sleeps. Otherwise the refill would happen whenever
     * the threshold happens to be crossed, and a Xoonglin sleeping every night could still wake
     * up tired. This way it always wakes up fully rested.
     */
    @Override
    public void unsatisfy(double frequency, XoonglinEntity mob) {
        if (mob.isSleeping()) {
            satisfaction = 1.0;
            return;
        }
        super.unsatisfy(frequency, mob);
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        mob.getBrain().setMemory(CftMemoryModuleType.MUST_SLEEP.get(), true);
    }
}
