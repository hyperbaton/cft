package com.hyperbaton.cft;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

@EventBusSubscriber(modid = CftMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CftConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue XOONGLIN_MATING_COOLDOWN = BUILDER
            .comment("Time for a Xoonglin to mate again, in ticks.")
            .defineInRange("xoonglinMatingCooldown", 6000, 0, 24000);

    public static final ModConfigSpec.IntValue MAX_HOUSE_SIZE = BUILDER
            .comment("Maximum amount of blocks that a Xoonglin house can have.")
            .defineInRange("homeDetection.maxHouseSize", 1000, 36, 24000);
    public static final ModConfigSpec.IntValue MAX_FLOOR_SIZE = BUILDER
            .comment("Maximum amount of blocks that the floor of a Xoonglin house can have.")
            .defineInRange("homeDetection.maxFloorSize", 100, 9, 1000);
    public static final ModConfigSpec.IntValue MAX_HOUSE_HEIGHT = BUILDER
            .comment("Maximum height of a block that is part of a Xoonglin house.")
            .defineInRange("homeDetection.maxHouseHeight", 319, -64, 319);
    public static final ModConfigSpec.BooleanValue KEEP_XOONGLINS_LOADED = BUILDER
            .comment("This will force loading the chunks where a Xoonglin is present. ATTENTION: This can highly damage the performance.")
            .define("keepXoonglinsLoaded", false);
    public static final ModConfigSpec.DoubleValue CLOSE_ENOUGH_DISTANCE_TO_CONTAINER = BUILDER
            .comment("Minimum distance at which a Xoonglin can interact with containers.")
            .defineInRange("needs.closeEnoughDistanceToContainer", 1.25, 0.1, 5);
    public static final ModConfigSpec.IntValue SUPPLY_COOLDOWN = BUILDER
            .comment("How long to check again if supplies are not present, in ticks.")
            .defineInRange("needs.supplyCooldown", 200, 40, 5000);
    public static final ModConfigSpec.DoubleValue HOME_WORK_RADIUS = BUILDER
            .comment("The distance from the home entrance that is considered near enough for working at home.")
            .defineInRange("jobs.homeWorkRadius", 3.0, 1.0, 50.0);
    public static final ModConfigSpec.BooleanValue USE_HUMANOID_MODEL = BUILDER
            .comment("If true, Xoonglins will use the basic humanoid/player model instead of the custom Xoonglin model.")
            .define("useHumanoidModel", false);
    static final ModConfigSpec SPEC = BUILDER.build();


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
    }
}
