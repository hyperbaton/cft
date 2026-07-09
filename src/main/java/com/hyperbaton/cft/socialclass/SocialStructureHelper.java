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

    /**
     * Raw population counts per class, as if the given Xoonglin had already moved from
     * fromClass to toClass. Used to preview an upgrade/downgrade before it happens.
     */
    public static Map<SocialClass, Integer> computeSocialStructureForPlayerWithUpgrade(
            ServerLevel level, ServerPlayer player, String fromClass, String toClass) {
        Map<SocialClass, Integer> socialStructure = computeSocialStructureForPlayer(level, player);
        SocialClass formerSocialClass = CftRegistry.SOCIAL_CLASSES.get(ResourceLocation.parse(fromClass));
        SocialClass nextSocialClass = CftRegistry.SOCIAL_CLASSES.get(ResourceLocation.parse(toClass));
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
    public static double computeScopedPercentage(Map<SocialClass, Integer> counts, String target, List<String> scope) {
        int numerator = counts.entrySet().stream()
                .filter(entry -> entry.getKey().getId().equals(target))
                .mapToInt(Map.Entry::getValue)
                .findFirst().orElse(0);

        int denominator;
        if (scope == null || scope.isEmpty()) {
            denominator = counts.values().stream().mapToInt(Integer::intValue).sum();
        } else {
            denominator = counts.entrySet().stream()
                    .filter(entry -> scope.contains(entry.getKey().getId()))
                    .mapToInt(Map.Entry::getValue)
                    .sum();
        }

        return denominator <= 0 ? 0.0 : (double) numerator / denominator;
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
