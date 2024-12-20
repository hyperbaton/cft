package com.hyperbaton.cft;

import com.hyperbaton.cft.creativetab.CreativeModTabs;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.ai.activity.CftActivities;
import com.hyperbaton.cft.entity.ai.sensor.CftSensorTypes;
import com.hyperbaton.cft.entity.client.XoonglinRenderer;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.event.CftDatapackRegistryEvents;
import com.hyperbaton.cft.item.CftItems;
import com.hyperbaton.cft.network.CftPacketHandler;
import com.hyperbaton.cft.sound.CftSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CftMod.MOD_ID)
public class CftMod
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "cft";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    public CftMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        CftRegistry.NEEDS_CODEC.register(modEventBus);

        CreativeModTabs.register(modEventBus);

        CftItems.register(modEventBus);

        CftEntities.register(modEventBus);

        CftMemoryModuleType.register(modEventBus);

        CftSensorTypes.register(modEventBus);

        CftActivities.register(modEventBus);

        CftSounds.register(modEventBus);

        // Register Needs
        modEventBus.register(new CftDatapackRegistryEvents());

        // Register the packets
        CftPacketHandler.init();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CftConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event)
    {
        CftRegistry.NEEDS = CftRegistry.getNeedsRegistry(event.getServer().registryAccess());
        CftRegistry.SOCIAL_CLASSES = CftRegistry.getSocialClassesRegistry(event.getServer().registryAccess());
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            EntityRenderers.register(CftEntities.XOONGLIN.get(), XoonglinRenderer::new);
        }
    }
}
