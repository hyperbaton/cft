package com.hyperbaton.cft.capability.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.socialclass.SocialClass;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NeedUtils {
    public static List<NeedCapability<? extends Need>> getNeedsForClass(SocialClass socialClass) {
        return socialClass.getNeeds().stream()
                .map(need -> CftRegistry.NEEDS.get(new ResourceLocation(need)))
                .filter(Objects::nonNull)
                .map(Need::createCapability)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
