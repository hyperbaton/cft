package com.hyperbaton.cft;

import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.job.BuilderJob;
import com.hyperbaton.cft.job.CrafterJob;
import com.hyperbaton.cft.job.FarmerJob;
import com.hyperbaton.cft.job.FisherJob;
import com.hyperbaton.cft.job.GathererJob;
import com.hyperbaton.cft.job.HealerJob;
import com.hyperbaton.cft.job.QuarryMinerJob;
import com.hyperbaton.cft.job.GuardJob;
import com.hyperbaton.cft.job.HaulerJob;
import com.hyperbaton.cft.job.HomeArtisanJob;
import com.hyperbaton.cft.job.OfficiantJob;
import com.hyperbaton.cft.need.*;
import com.hyperbaton.cft.event.CftDatapackRegistryEvents;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.type.CompoundStructureType;
import com.hyperbaton.cft.structure.type.EnclosedBuildingStructureType;
import com.hyperbaton.cft.structure.type.HouseStructureType;
import com.hyperbaton.cft.structure.type.MonumentStructureType;
import com.hyperbaton.cft.structure.type.MultiStoreyBuildingStructureType;
import com.hyperbaton.cft.structure.type.OpenAirPlatformStructureType;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.TicketType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.Comparator;
import java.util.UUID;


public class CftRegistry {

    public static final TicketType<UUID> XOONGLIN_CHUNK_TICKET = TicketType.create(
            "xoonglin_chunk_ticket", Comparator.comparing(UUID::toString)
    );

    public static Registry<Need> NEEDS;
    public static Registry<SocialClass> SOCIAL_CLASSES;
    public static Registry<Job> JOBS;
    public static Registry<StructureType> STRUCTURES;

    public static final Registry<Codec<? extends Need>> NEEDS_CODEC_REGISTRY =
            new RegistryBuilder<>(CftDatapackRegistryEvents.NEED_CODEC_KEY).create();

    public static final Registry<Codec<? extends Job>> JOBS_CODEC_REGISTRY =
            new RegistryBuilder<>(CftDatapackRegistryEvents.JOB_CODEC_KEY).create();

    public static final Registry<Codec<? extends StructureType>> STRUCTURE_TYPE_CODEC_REGISTRY =
            new RegistryBuilder<>(CftDatapackRegistryEvents.STRUCTURE_TYPE_CODEC_KEY).create();

    public static final DeferredRegister<Codec<? extends Need>> NEEDS_CODEC =
            DeferredRegister.create(CftDatapackRegistryEvents.NEED_CODEC_KEY, CftMod.MOD_ID);

    public static final DeferredHolder<Codec<? extends Need>, Codec<HomeNeed>> HOME_NEED = NEEDS_CODEC.register("home", () -> HomeNeed.HOME_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<GoodsNeed>> GOODS_NEED = NEEDS_CODEC.register("goods", () -> GoodsNeed.GOODS_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<AltitudeNeed>> ALTITUDE_NEED = NEEDS_CODEC.register("altitude", () -> AltitudeNeed.ALTITUDE_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<BiomeNeed>> BIOME_NEED = NEEDS_CODEC.register("biome", () -> BiomeNeed.BIOME_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<FluidNeed>> FLUID_NEED = NEEDS_CODEC.register("fluid", () -> FluidNeed.FLUID_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<EnergyNeed>> ENERGY_NEED = NEEDS_CODEC.register("energy", () -> EnergyNeed.ENERGY_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<SocialNeed>> SOCIAL_NEED = NEEDS_CODEC.register("social", () -> SocialNeed.SOCIAL_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<LightingNeed>> LIGHTING_NEED = NEEDS_CODEC.register("lighting", () -> LightingNeed.LIGHTING_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<PetNeed>> PET_NEED = NEEDS_CODEC.register("pet", () -> PetNeed.PET_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<DecorationNeed>> DECORATION_NEED = NEEDS_CODEC.register("decoration", () -> DecorationNeed.DECORATION_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<StructureNeed>> STRUCTURE_NEED = NEEDS_CODEC.register("structure", () -> StructureNeed.STRUCTURE_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<EquipmentNeed>> EQUIPMENT_NEED = NEEDS_CODEC.register("equipment", () -> EquipmentNeed.EQUIPMENT_NEED_CODEC);
    public static final DeferredHolder<Codec<? extends Need>, Codec<RitualNeed>> RITUAL_NEED = NEEDS_CODEC.register("ritual", () -> RitualNeed.RITUAL_NEED_CODEC);

    public static final DeferredRegister<Codec<? extends Job>> JOBS_CODEC =
            DeferredRegister.create(CftDatapackRegistryEvents.JOB_CODEC_KEY, CftMod.MOD_ID);

    public static final DeferredHolder<Codec<? extends Job>, Codec<HomeArtisanJob>> HOME_ARTISAN_JOB =
            JOBS_CODEC.register("home_artisan", () -> HomeArtisanJob.CODEC);

    public static final DeferredHolder<Codec<? extends Job>, Codec<GathererJob>> GATHERER_JOB =
            JOBS_CODEC.register("gatherer", () -> GathererJob.CODEC);

    public static final DeferredHolder<Codec<? extends Job>, Codec<GuardJob>> GUARD_JOB =
            JOBS_CODEC.register("guard", () -> GuardJob.CODEC);

    public static final DeferredHolder<Codec<? extends Job>, Codec<FarmerJob>> FARMER_JOB =
            JOBS_CODEC.register("farmer", () -> FarmerJob.CODEC);

    public static final DeferredHolder<Codec<? extends Job>, Codec<HaulerJob>> HAULER_JOB =
            JOBS_CODEC.register("hauler", () -> HaulerJob.CODEC);

    public static final DeferredHolder<Codec<? extends Job>, Codec<BuilderJob>> BUILDER_JOB =
            JOBS_CODEC.register("builder", () -> BuilderJob.CODEC);

    public static final DeferredHolder<Codec<? extends Job>, Codec<OfficiantJob>> OFFICIANT_JOB =
            JOBS_CODEC.register("officiant", () -> OfficiantJob.CODEC);

    public static final DeferredHolder<Codec<? extends Job>, Codec<CrafterJob>> CRAFTER_JOB =
            JOBS_CODEC.register("crafter", () -> CrafterJob.CODEC);

    public static final DeferredHolder<Codec<? extends Job>, Codec<FisherJob>> FISHER_JOB =
            JOBS_CODEC.register("fisher", () -> FisherJob.CODEC);

    public static final DeferredHolder<Codec<? extends Job>, Codec<HealerJob>> HEALER_JOB =
            JOBS_CODEC.register("healer", () -> HealerJob.CODEC);

    public static final DeferredHolder<Codec<? extends Job>, Codec<QuarryMinerJob>> QUARRY_MINER_JOB =
            JOBS_CODEC.register("quarry_miner", () -> QuarryMinerJob.CODEC);

    public static final DeferredRegister<Codec<? extends StructureType>> STRUCTURE_TYPE_CODECS =
            DeferredRegister.create(CftDatapackRegistryEvents.STRUCTURE_TYPE_CODEC_KEY, CftMod.MOD_ID);

    public static final DeferredHolder<Codec<? extends StructureType>, Codec<EnclosedBuildingStructureType>> ENCLOSED_BUILDING_STRUCTURE_TYPE =
            STRUCTURE_TYPE_CODECS.register("enclosed_building", () -> EnclosedBuildingStructureType.CODEC);

    public static final DeferredHolder<Codec<? extends StructureType>, Codec<HouseStructureType>> HOUSE_STRUCTURE_TYPE =
            STRUCTURE_TYPE_CODECS.register("house", () -> HouseStructureType.CODEC);

    public static final DeferredHolder<Codec<? extends StructureType>, Codec<OpenAirPlatformStructureType>> OPEN_AIR_PLATFORM_STRUCTURE_TYPE =
            STRUCTURE_TYPE_CODECS.register("open_air_platform", () -> OpenAirPlatformStructureType.CODEC);

    public static final DeferredHolder<Codec<? extends StructureType>, Codec<MonumentStructureType>> MONUMENT_STRUCTURE_TYPE =
            STRUCTURE_TYPE_CODECS.register("monument", () -> MonumentStructureType.CODEC);

    public static final DeferredHolder<Codec<? extends StructureType>, Codec<MultiStoreyBuildingStructureType>> MULTI_STOREY_BUILDING_STRUCTURE_TYPE =
            STRUCTURE_TYPE_CODECS.register("multi_storey_building", () -> MultiStoreyBuildingStructureType.CODEC);

    public static final DeferredHolder<Codec<? extends StructureType>, Codec<CompoundStructureType>> COMPOUND_STRUCTURE_TYPE =
            STRUCTURE_TYPE_CODECS.register("compound", () -> CompoundStructureType.CODEC);

    public static Registry<Need> getNeedsRegistry(RegistryAccess registryAccess) {
        return registryAccess.registryOrThrow(CftDatapackRegistryEvents.NEED_KEY);
    }

    public static Registry<SocialClass> getSocialClassesRegistry(RegistryAccess registryAccess) {
        return registryAccess.registryOrThrow(CftDatapackRegistryEvents.SOCIAL_CLASS_KEY);
    }

    public static Registry<Job> getJobsRegistry(RegistryAccess registryAccess) {
        return registryAccess.registryOrThrow(CftDatapackRegistryEvents.JOB_KEY);
    }

    public static Registry<StructureType> getStructureTypesRegistry(RegistryAccess registryAccess) {
        return registryAccess.registryOrThrow(CftDatapackRegistryEvents.STRUCTURE_TYPE_KEY);
    }
}
