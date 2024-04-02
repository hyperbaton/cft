package com.hyperbaton.cft.event;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.capability.need.Need;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DataPackRegistryEvent;
import org.slf4j.Logger;

public class CftDatapackRegistryEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceKey<Registry<Need>> NEED_KEY = CftDatapackRegistryEvents.createRegistryKey("need");
    public static final ResourceKey<Registry<SocialClass>> SOCIAL_CLASS_KEY = CftDatapackRegistryEvents.createRegistryKey("socialclass");
    public static final ResourceKey<Registry<Codec<? extends Need>>> NEED_CODEC_KEY = CftDatapackRegistryEvents.createRegistryKey("need_serializer");

    @SubscribeEvent
    public void newDatapackRegistry(DataPackRegistryEvent.NewRegistry event) {
        LOGGER.info("Registering custom registries");
        event.dataPackRegistry(NEED_KEY, Need.NEED_CODEC, Need.NEED_CODEC);
        event.dataPackRegistry(SOCIAL_CLASS_KEY, SocialClass.SOCIAL_CLASS_CODEC, SocialClass.SOCIAL_CLASS_CODEC);
    }

    private static <T> ResourceKey<Registry<T>> createRegistryKey(java.lang.String name) {
        return ResourceKey.createRegistryKey(new ResourceLocation(CftMod.MOD_ID, name));
    }
}
