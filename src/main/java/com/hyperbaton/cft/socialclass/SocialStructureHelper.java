package com.hyperbaton.cft.socialclass;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class SocialStructureHelper {

    public static Map<SocialClass, Integer> computeSocialStructureForPlayer(ServerLevel level, ServerPlayer player) {
        Map<SocialClass, Integer> socialStructure = new HashMap<>();
        List<XoonglinEntity> xoonglinList = getAllXoonglins(level);
        for (XoonglinEntity xoonglin : xoonglinList) {
            if (xoonglin.getLeaderId() != null &&
                    xoonglin.getLeaderId().equals(player.getUUID())) {
                if (socialStructure.containsKey(xoonglin.getSocialClass())) {
                    socialStructure.replace(xoonglin.getSocialClass(), socialStructure.get(xoonglin.getSocialClass()) + 1);
                } else {
                    socialStructure.put(xoonglin.getSocialClass(), 1);
                }
            }
        }
        return socialStructure;
    }

    public static Map<SocialClass, BigDecimal> computeNormalizedSocialStructureForPlayer(ServerLevel level, ServerPlayer player) {
        Map<SocialClass, Integer> socialStructure = computeSocialStructureForPlayer(level, player);
        int population = socialStructure.values().stream().reduce(0, Integer::sum);
        Map<SocialClass, BigDecimal> normalizedSocialStructure = new HashMap<>();
        socialStructure.forEach((key, value) -> normalizedSocialStructure.put(key, BigDecimal.valueOf(value)
                .setScale(8, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(population).setScale(8, RoundingMode.HALF_UP), RoundingMode.HALF_UP)));
        return normalizedSocialStructure;
    }

    public static Map<SocialClass, BigDecimal> computeNormalizedSocialStructureForPlayerWithUpgrade(ServerLevel level, ServerPlayer player, String fromClass, String toClass) {
        Map<SocialClass, Integer> socialStructure = computeSocialStructureForPlayer(level, player);
        int population = socialStructure.values().stream().reduce(0, Integer::sum);
        SocialClass formerSocialClass = CftRegistry.SOCIAL_CLASSES.get(new ResourceLocation(fromClass));
        SocialClass nextSocialClass = CftRegistry.SOCIAL_CLASSES.get(new ResourceLocation(toClass));
        // Reduce the population of previous class by 1, to account for the change
        socialStructure.replace(formerSocialClass, socialStructure.get(formerSocialClass) - 1);
        // Increase the population of next class by 1, to account for the change
        if (socialStructure.get(nextSocialClass) == null) {
            socialStructure.put(nextSocialClass, 1);
        } else {
            socialStructure.replace(nextSocialClass, socialStructure.get(nextSocialClass) + 1);
        }
        Map<SocialClass, BigDecimal> normalizedSocialStructure = new HashMap<>();
        socialStructure.forEach((key, value) -> normalizedSocialStructure.put(key, BigDecimal.valueOf(value)
                .setScale(8, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(population).setScale(8, RoundingMode.HALF_UP), RoundingMode.HALF_UP)));
        return normalizedSocialStructure;
    }

    public static List<XoonglinEntity> getAllXoonglins(ServerLevel level) {
        Iterator<Entity> entityIterator = level.getEntities().getAll().iterator();
        List<XoonglinEntity> xoonglinList = new ArrayList<>();
        while (entityIterator.hasNext()) {
            Entity entity = entityIterator.next();
            if (entity instanceof XoonglinEntity) {
                xoonglinList.add((XoonglinEntity) entity);
            }
        }
        return xoonglinList;
    }
}
