package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.SocialNeed;
import com.mojang.logging.LogUtils;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.List;

public class SocialNeedSatisfier extends NeedSatisfier<SocialNeed> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public SocialNeedSatisfier(double satisfaction, boolean isSatisfied, SocialNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        SocialNeed need = getNeed();

        int radius = Math.max(0, need.getRadius());
        // Resolve min/max with sensible defaults and compatibility with old "count"
        int min = Math.max(0, need.getMinCount());
        int max = need.getMaxCount();
        if (max < min) {
            max = min; // keep sane bounds
        }

        List<String> acceptable = need.getAcceptedSocialClassIds();


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

        LOGGER.trace("Xoonglins found nearby {}: {}. Minimum and maximum: {}/{}", mob.getName(), matching, min, max);
        // Satisfied only if within [min, max], inclusive
        if (matching >= min && matching <= max) {
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