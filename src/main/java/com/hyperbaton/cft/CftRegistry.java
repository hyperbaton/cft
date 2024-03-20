package com.hyperbaton.cft;

import com.hyperbaton.cft.capability.need.GoodsNeed;
import com.hyperbaton.cft.event.CftDatapackRegistryEvents;
import com.hyperbaton.cft.socialclass.SocialClass;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;


public class CftRegistry {

    public static Registry<GoodsNeed> GOODS_NEEDS;
    public static Registry<SocialClass> SOCIAL_CLASSES;

    public static Registry<GoodsNeed> getNeedsRegistry(RegistryAccess registryAccess){
        return registryAccess.registryOrThrow(CftDatapackRegistryEvents.GOODS_NEED_KEY);
    }
    public static Registry<SocialClass> getSocialClassesRegistry(RegistryAccess registryAccess){
        return registryAccess.registryOrThrow(CftDatapackRegistryEvents.SOCIAL_CLASS_KEY);
    }
}
