package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.AltitudeNeed;

public class AltitudeNeedSatisfier extends NeedSatisfier<AltitudeNeed> {
    public AltitudeNeedSatisfier(double satisfaction, boolean isSatisfied, AltitudeNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (mob.getY() > need.getMinAltitude() && mob.getY() < need.getMaxAltitude()) {
            super.satisfy(mob);
        } else {
            this.unsatisfy(need.getFrequency(), mob);
            mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
            addMemoriesForSatisfaction(mob);
            return false;
        }
        return true;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {

    }
}
