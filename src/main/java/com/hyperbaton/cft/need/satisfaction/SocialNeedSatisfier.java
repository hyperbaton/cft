package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.SocialNeed;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SocialNeedSatisfier extends NeedSatisfier<SocialNeed> {

    public SocialNeedSatisfier(double satisfaction, boolean isSatisfied, SocialNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        SocialNeed need = getNeed();

        int radius = Math.max(0, need.getRadius());
        int required = Math.max(0, need.getRequiredCount());
        List<String> acceptable = need.getAcceptedSocialClassIds();

        // If nothing is required, satisfy immediately through the base class
        if (required == 0) {
            super.satisfy(mob);
            return true;
        }

        // Look for nearby Xoonglins within the radius, using a box centered on the mob.
        AABB area = new AABB(mob.blockPosition()).inflate(radius);
        List<XoonglinEntity> nearby = mob.level().getEntitiesOfClass(
                XoonglinEntity.class,
                area,
                e -> e != mob && e.isAlive()
        );

        // Count only those that match one of the accepted social class IDs
        long matching = nearby.stream()
                .filter(nearbyXoonglin -> nearbyXoonglin.getLeaderId().equals(mob.getLeaderId()))
                .filter(e -> {
                    if (acceptable == null || acceptable.isEmpty()) return true;
                    String classId = e.getSocialClass().getId();
                    return classId != null && acceptable.contains(classId);
                }).count();

        if (matching >= required) {
            super.satisfy(mob);
            return true;
        }

        // Not enough matching Xoonglins: unsatisfy
        this.unsatisfy(need.getFrequency(), mob);
        mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
        addMemoriesForSatisfaction(mob);
        return false;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
    }
}