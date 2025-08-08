package com.hyperbaton.cft;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = CftMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CftConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue XOONGLIN_MATING_COOLDOWN = BUILDER
            .comment("Time for a Xoonglin to mate again, in ticks.")
            .defineInRange("xoonglinMatingCooldown", 6000, 0, 24000);
    //public static final ForgeConfigSpec.IntValue MAX_RANGE;

    public static final ForgeConfigSpec.IntValue MAX_HOUSE_SIZE = BUILDER
            .comment("Maximum amount of blocks that a Xoonglin house can have.")
            .defineInRange("homeDetection.maxHouseSize", 1000, 36, 24000);
    public static final ForgeConfigSpec.IntValue MAX_FLOOR_SIZE = BUILDER
            .comment("Maximum amount of blocks that the floor of a Xoonglin house can have.")
            .defineInRange("homeDetection.maxFloorSize", 100, 9, 1000);
    public static final ForgeConfigSpec.IntValue MAX_HOUSE_HEIGHT = BUILDER
            .comment("Maximum height of a block that is part of a Xoonglin house.")
            .defineInRange("homeDetection.maxHouseSize", 319, -64, 319);
    public static final ForgeConfigSpec.BooleanValue KEEP_XOONGLINS_LOADED = BUILDER
            .comment("This will force loading the chunks where a Xoonglin is present. ATTENTION: This can highly damage the performance.")
            .define("keepXoonglinsLoaded", false);
    public static final ForgeConfigSpec.DoubleValue CLOSE_ENOUGH_DISTANCE_TO_CONTAINER = BUILDER
            .comment("Minimum distance at which a Xoonglin can interact with containers.")
            .defineInRange("needs.closeEnoughDistanceToContainer", 1.25, 0.1, 5);
    public static final ForgeConfigSpec.IntValue SUPPLY_COOLDOWN = BUILDER
            .comment("How long to check again if supplies are not present, in ticks.")
            .defineInRange("needs.supplyCooldown", 200, 40, 5000);
    public static final ForgeConfigSpec.DoubleValue HOME_WORK_RADIUS = BUILDER
            .comment("The distance from the home entrance that is considered near enough for working at home.")
            .defineInRange("jobs.homeWorkRadius", 6.0, 1.0, 50.0);
    static final ForgeConfigSpec SPEC = BUILDER.build();


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
    }
}
