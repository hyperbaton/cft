package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.RitualNeed;
import com.hyperbaton.cft.ritual.Ritual;
import com.hyperbaton.cft.world.RitualsData;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/**
 * Unlike most needs, a ritual need cannot satisfy itself: satisfaction arrives from
 * outside, via onRitualCompleted(), when an officiant finishes a matching ritual nearby.
 * satisfy() only steers the xoonglin toward an ongoing ritual when presence is required.
 */
public class RitualNeedSatisfier extends NeedSatisfier<RitualNeed> {

    public RitualNeedSatisfier(double satisfaction, boolean isSatisfied, RitualNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (mob.level().isClientSide) {
            return isSatisfied();
        }

        addMemoriesForSatisfaction(mob);

        this.unsatisfy(need.getFrequency(), mob);
        mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
        return false;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        if (!need.isRequiresPresence() || mob.getLeaderId() == null) {
            return;
        }
        // An officiant performing a ritual cannot attend another one
        if (mob.getBrain().hasMemoryValue(CftMemoryModuleType.MUST_PERFORM_RITUAL.get())) {
            return;
        }
        ServerLevel level = (ServerLevel) mob.level();
        RitualsData data = level.getDataStorage().computeIfAbsent(RitualsData.factory(), "ritualsData");
        Optional<Ritual> ritual = data.findNearby(need.getRitualId(), mob.getLeaderId(),
                mob.blockPosition(), need.getRadius());
        // Only join rituals that are still gathering; joining mid-performance cannot
        // satisfy a presence need (attendance is required from beginning to end)
        if (ritual.isPresent() && ritual.get().getState() == Ritual.State.GATHERING) {
            mob.getBrain().setMemory(CftMemoryModuleType.MUST_ATTEND_RITUAL.get(), ritual.get().getCenter());
        }
    }

    /**
     * Called by PerformRitualBehavior when a matching ritual completes and this
     * xoonglin qualifies (present throughout, or merely nearby if presence not required).
     */
    public void onRitualCompleted(XoonglinEntity mob) {
        super.satisfy(mob);
        setSatisfied(true);
    }
}
