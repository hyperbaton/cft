package com.hyperbaton.cft;

import com.hyperbaton.cft.creativetab.CreativeModTabs;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.ai.activity.CftActivities;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.ai.sensor.CftSensorTypes;
import com.hyperbaton.cft.entity.client.XoonglinRenderer;
import com.hyperbaton.cft.event.CftDatapackRegistryEvents;
import com.hyperbaton.cft.item.CftItems;
import com.hyperbaton.cft.network.CftPacketHandler;
import com.hyperbaton.cft.sound.CftSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import org.slf4j.Logger;

@Mod(CftMod.MOD_ID)
public class CftMod
{
    public static final String MOD_ID = "cft";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CftMod(IEventBus modEventBus, ModContainer modContainer)
    {
        CftRegistry.NEEDS_CODEC.register(modEventBus);

        CftRegistry.JOBS_CODEC.register(modEventBus);

        CftRegistry.STRUCTURE_TYPE_CODECS.register(modEventBus);

        CreativeModTabs.register(modEventBus);

        CftItems.register(modEventBus);

        CftEntities.register(modEventBus);

        CftMemoryModuleType.register(modEventBus);

        CftSensorTypes.register(modEventBus);

        CftActivities.register(modEventBus);

        CftSounds.register(modEventBus);

        modEventBus.register(new CftDatapackRegistryEvents());

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerRegistries);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, CftConfig.SPEC);
    }

    private void registerRegistries(NewRegistryEvent event) {
        event.register(CftRegistry.NEEDS_CODEC_REGISTRY);
        event.register(CftRegistry.JOBS_CODEC_REGISTRY);
        event.register(CftRegistry.STRUCTURE_TYPE_CODEC_REGISTRY);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event)
    {
        CftRegistry.NEEDS = CftRegistry.getNeedsRegistry(event.getServer().registryAccess());
        CftRegistry.SOCIAL_CLASSES = CftRegistry.getSocialClassesRegistry(event.getServer().registryAccess());
        CftRegistry.JOBS = CftRegistry.getJobsRegistry(event.getServer().registryAccess());
        CftRegistry.STRUCTURES = CftRegistry.getStructureTypesRegistry(event.getServer().registryAccess());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            EntityRenderers.register(CftEntities.XOONGLIN.get(), XoonglinRenderer::new);
        }
    }
}
