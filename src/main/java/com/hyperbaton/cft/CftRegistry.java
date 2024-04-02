package com.hyperbaton.cft;

import com.hyperbaton.cft.capability.need.GoodsNeed;
import com.hyperbaton.cft.capability.need.HomeNeed;
import com.hyperbaton.cft.capability.need.Need;
import com.hyperbaton.cft.event.CftDatapackRegistryEvents;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;


public class CftRegistry {

    public static Registry<Need> NEEDS;
    public static Registry<SocialClass> SOCIAL_CLASSES;

    public static final DeferredRegister<Codec<? extends Need>> NEEDS_CODEC =
            DeferredRegister.create(CftDatapackRegistryEvents.NEED_CODEC_KEY, CftMod.MOD_ID);
    public static final Supplier<IForgeRegistry<Codec<? extends Need>>> NEEDS_CODEC_SUPPLIER = NEEDS_CODEC.makeRegistry(RegistryBuilder::new);

    public static final RegistryObject<Codec<HomeNeed>> HOME_NEED = NEEDS_CODEC.register("home", () -> HomeNeed.HOME_NEED_CODEC);
    public static final RegistryObject<Codec<GoodsNeed>> GOODS_NEED = NEEDS_CODEC.register("goods", () -> GoodsNeed.GOODS_NEED_CODEC);

    public static Registry<Need> getNeedsRegistry(RegistryAccess registryAccess){
        return registryAccess.registryOrThrow(CftDatapackRegistryEvents.NEED_KEY);
    }
    public static Registry<SocialClass> getSocialClassesRegistry(RegistryAccess registryAccess){
        return registryAccess.registryOrThrow(CftDatapackRegistryEvents.SOCIAL_CLASS_KEY);
    }
}
