package com.hyperbaton.cft.util;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.socialclass.SocialClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class SocialStructureUtil {

    public static Map<SocialClass, Integer> computeSocialStructureForPlayer(ServerLevel level, ServerPlayer player) {
        Map<SocialClass, Integer> socialStructure = new HashMap<>();
        for (XoonglinEntity xoonglin : getLeaderXoonglins(level, player.getUUID())) {
            socialStructure.merge(xoonglin.getSocialClass(), 1, Integer::sum);
        }
        return socialStructure;
    }

    /**
     * Raw population counts per class, as if the given Xoonglin had already moved from
     * fromClass to toClass. Used to preview an upgrade/downgrade before it happens.
     */
    public static Map<SocialClass, Integer> computeSocialStructureForPlayerWithUpgrade(
            ServerLevel level, ServerPlayer player, ResourceLocation fromClass, ResourceLocation toClass) {
        Map<SocialClass, Integer> socialStructure = computeSocialStructureForPlayer(level, player);
        SocialClass formerSocialClass = CftRegistry.SOCIAL_CLASSES.get(fromClass);
        SocialClass nextSocialClass = CftRegistry.SOCIAL_CLASSES.get(toClass);
        // Reduce the population of previous class by 1, to account for the change
        socialStructure.replace(formerSocialClass, socialStructure.get(formerSocialClass) - 1);
        // Increase the population of next class by 1, to account for the change
        socialStructure.merge(nextSocialClass, 1, Integer::sum);
        return socialStructure;
    }

    /**
     * The share of `target` within `counts`, restricted to `scope` (a list of social
     * class IDs). If scope is null or empty, the share is computed against the whole
     * population instead of a restricted set of classes.
     */
    public static double computeScopedPercentage(Map<SocialClass, Integer> counts, ResourceLocation target, List<ResourceLocation> scope) {
        int numerator = counts.entrySet().stream()
                .filter(entry -> CftRegistry.getSocialClassId(entry.getKey()).equals(target))
                .mapToInt(Map.Entry::getValue)
                .findFirst().orElse(0);

        int denominator;
        if (scope == null || scope.isEmpty()) {
            denominator = counts.values().stream().mapToInt(Integer::intValue).sum();
        } else {
            denominator = counts.entrySet().stream()
                    .filter(entry -> scope.contains(CftRegistry.getSocialClassId(entry.getKey())))
                    .mapToInt(Map.Entry::getValue)
                    .sum();
        }

        return denominator <= 0 ? 0.0 : (double) numerator / denominator;
    }

    /**
     * The Xoonglins loaded in this level that follow the given leader. Those without a social class
     * yet are left out.
     */
    public static List<XoonglinEntity> getLeaderXoonglins(ServerLevel level, UUID leaderId) {
        return getAllXoonglins(level).stream()
                .filter(xoonglin -> leaderId.equals(xoonglin.getLeaderId()) && xoonglin.getSocialClass() != null)
                .toList();
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
