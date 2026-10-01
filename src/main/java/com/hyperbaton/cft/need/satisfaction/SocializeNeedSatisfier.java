package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.SocializeNeed;

/**
 * Like the ritual need, a socialize need cannot satisfy itself: satisfaction arrives from
 * outside, via onSocialized(), when the Xoonglin has spent time with another one (for now, when
 * ConverseBehavior finishes a conversation). satisfy() only sets MUST_SOCIALIZE so the Xoonglin
 * looks for company.
 */
public class SocializeNeedSatisfier extends NeedSatisfier<SocializeNeed> {

    public SocializeNeedSatisfier(double satisfaction, boolean isSatisfied, SocializeNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (mob.level().isClientSide) {
            return isSatisfied();
        }

        addMemoriesForSatisfaction(mob);
        return fail(mob);
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        mob.getBrain().setMemory(CftMemoryModuleType.MUST_SOCIALIZE.get(), true);
    }

    /**
     * Called when the Xoonglin has spent time with a Xoonglin this need accepts (for now, by
     * ConverseBehavior at the end of a conversation). Clears MUST_SOCIALIZE; another socialize
     * need that is still unsatisfied will set it again on its next check.
     */
    public void onSocialized(XoonglinEntity mob) {
        super.satisfy(mob);
        setSatisfied(true);
        mob.getBrain().eraseMemory(CftMemoryModuleType.MUST_SOCIALIZE.get());
    }
}
