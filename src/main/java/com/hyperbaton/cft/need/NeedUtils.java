package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.ReadingNeedSatisfier;
import com.hyperbaton.cft.network.NeedSatisfactionData;
import com.hyperbaton.cft.socialclass.SocialClass;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class NeedUtils {
    /** Builds the network-facing snapshot of a Xoonglin's non-hidden needs. */
    public static Map<String, NeedSatisfactionData> buildNeedsData(XoonglinEntity xoonglin) {
        Map<String, NeedSatisfactionData> result = new HashMap<>();
        for (NeedSatisfier<? extends Need> satisfier : xoonglin.getNeeds()) {
            if (satisfier.getNeed().isHidden()) continue;

            String extraTooltip = null;
            if (satisfier instanceof ReadingNeedSatisfier readingSatisfier) {
                extraTooltip = readingSatisfier.wantedBookEntry(xoonglin)
                        .map(entry -> entry.title() + " by " + entry.authorName())
                        .orElse(null);
            }

            result.put(satisfier.getNeed().getId(), new NeedSatisfactionData(
                    satisfier.getSatisfaction(),
                    satisfier.getNeed().getDamageThreshold(),
                    satisfier.getNeed().getSatisfactionThreshold(),
                    satisfier.getNeed().getIcons(),
                    extraTooltip
            ));
        }
        return result;
    }

    public static List<NeedSatisfier<? extends Need>> getNeedsForClass(SocialClass socialClass) {
        return socialClass.getNeeds().stream()
                .map(need -> CftRegistry.NEEDS.get(ResourceLocation.parse(need)))
                .filter(Objects::nonNull)
                .map(Need::createSatisfier)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static boolean classMeetsStructureType(SocialClass socialClass, String structureTypeId) {
        if (CftRegistry.NEEDS == null) return false;
        return socialClass.getNeeds().stream()
                .map(needId -> CftRegistry.NEEDS.get(ResourceLocation.parse(needId)))
                .filter(Objects::nonNull)
                .filter(need -> need instanceof HomeNeed)
                .map(need -> (HomeNeed) need)
                .anyMatch(homeNeed -> homeNeed.getRequiredStructure().equals(structureTypeId));
    }
}
