package com.hyperbaton.cft.capability.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.socialclass.SocialClass;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class NeedUtils {
    public static List<NeedCapability> getNeedsForClass(SocialClass socialClass) {
        return socialClass.getNeeds().stream()
                .map(need -> CftRegistry.NEEDS.get(new ResourceLocation(need)))
                .map(NeedUtils::buildNeedCapability)
                .toList();
    }

    // TODO: Better generalize this method for any type of Need
    private static NeedCapability buildNeedCapability(Need need) {
        if (need instanceof GoodsNeed) {
            return new ConsumeItemNeedCapability(need.getSatisfactionThreshold(), false, (GoodsNeed) need);
        } else if (need instanceof HomeNeed) {
            return new HomeNeedCapability(need.getSatisfactionThreshold(), false, (HomeNeed) need);
        }
        return null;
    }
}
