package com.hyperbaton.cft;

import com.hyperbaton.cft.need.*;
import com.hyperbaton.cft.event.CftDatapackRegistryEvents;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.TicketType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.Comparator;
import java.util.UUID;
import java.util.function.Supplier;


public class CftRegistry {

    public static final TicketType<UUID> XOONGLIN_CHUNK_TICKET = TicketType.create(
            "xoonglin_chunk_ticket", Comparator.comparing(UUID::toString)
    );

    public static Registry<Need> NEEDS;
    public static Registry<SocialClass> SOCIAL_CLASSES;

    public static final DeferredRegister<Codec<? extends Need>> NEEDS_CODEC =
            DeferredRegister.create(CftDatapackRegistryEvents.NEED_CODEC_KEY, CftMod.MOD_ID);
    public static final Supplier<IForgeRegistry<Codec<? extends Need>>> NEEDS_CODEC_SUPPLIER = NEEDS_CODEC.makeRegistry(RegistryBuilder::new);

    public static final RegistryObject<Codec<HomeNeed>> HOME_NEED = NEEDS_CODEC.register("home", () -> HomeNeed.HOME_NEED_CODEC);
    public static final RegistryObject<Codec<GoodsNeed>> GOODS_NEED = NEEDS_CODEC.register("goods", () -> GoodsNeed.GOODS_NEED_CODEC);
    public static final RegistryObject<Codec<AltitudeNeed>> ALTITUDE_NEED = NEEDS_CODEC.register("altitude", () -> AltitudeNeed.ALTITUDE_NEED_CODEC);
    public static final RegistryObject<Codec<BiomeNeed>> BIOME_NEED = NEEDS_CODEC.register("biome", () -> BiomeNeed.BIOME_NEED_CODEC);

    public static Registry<Need> getNeedsRegistry(RegistryAccess registryAccess){
        return registryAccess.registryOrThrow(CftDatapackRegistryEvents.NEED_KEY);
    }
    public static Registry<SocialClass> getSocialClassesRegistry(RegistryAccess registryAccess){
        return registryAccess.registryOrThrow(CftDatapackRegistryEvents.SOCIAL_CLASS_KEY);
    }
}
