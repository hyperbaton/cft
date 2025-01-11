package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.socialclass.SocialClass;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NeedUtils {
    public static List<NeedSatisfier<? extends Need>> getNeedsForClass(SocialClass socialClass) {
        return socialClass.getNeeds().stream()
                .map(need -> CftRegistry.NEEDS.get(new ResourceLocation(need)))
                .filter(Objects::nonNull)
                .map(Need::createSatisfier)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
