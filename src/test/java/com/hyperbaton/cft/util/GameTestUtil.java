package com.hyperbaton.cft.util;

import com.google.gson.JsonParser;
import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.spawner.XoonglinSpawner;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfierMapper;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureDetectionReason;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.home.HouseStructure;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared setup for CFT's game tests (in {@code com.hyperbaton.cft.gametest}): building
 * structures block by block in the empty template, detecting them, and spawning Xoonglins.
 * Positions are relative to the test, as {@link GameTestHelper} takes them.
 */
public final class GameTestUtil {
    /** The template every test uses: a 10x10x10 empty space, built in by the test itself. */
    public static final String EMPTY_TEMPLATE = "empty";

    public static final ResourceLocation CITIZEN_HOUSE = cftId("citizen_house");
    public static final ResourceLocation SETTLER_HOUSE = cftId("settler_house");
    public static final ResourceLocation BAKERY = cftId("bakery");
    public static final ResourceLocation CITIZEN = cftId("citizen");
    public static final ResourceLocation SETTLER = cftId("settler");

    /** Where {@link #buildCitizenHouse} builds, the floor's corner. */
    public static final BlockPos HOUSE_CORNER = new BlockPos(2, 1, 2);
    /** The lower half of the house's door: its key block. */
    public static final BlockPos HOUSE_DOOR = HOUSE_CORNER.offset(2, 1, 0);
    public static final BlockPos HOUSE_CHEST = HOUSE_CORNER.offset(3, 1, 3);
    /** A wall block of the house, away from the door. */
    public static final BlockPos HOUSE_WALL = HOUSE_CORNER.offset(0, 2, 2);
    /** The bakery's smoker, its key block. */
    public static final BlockPos BAKERY_SMOKER = HOUSE_CORNER.offset(3, 1, 3);

    private GameTestUtil() {
    }

    public static ResourceLocation cftId(String path) {
        return ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, path);
    }

    /**
     * A citizen house at {@link #HOUSE_CORNER}: a 5x5 stone floor, cobblestone walls three blocks
     * high with an oak door ({@link #HOUSE_DOOR}), a chest inside ({@link #HOUSE_CHEST}) and a
     * stone brick roof.
     */
    public static void buildCitizenHouse(GameTestHelper helper) {
        buildBox(helper, HOUSE_CORNER, Blocks.STONE, Blocks.COBBLESTONE, Blocks.STONE_BRICKS);
        placeDoor(helper, HOUSE_DOOR);
        helper.setBlock(HOUSE_CHEST, Blocks.CHEST);
    }

    /** A settler house where {@link #buildCitizenHouse} builds: plank floor, log walls and a roof of stairs. */
    public static void buildSettlerHouse(GameTestHelper helper) {
        buildBox(helper, HOUSE_CORNER, Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.OAK_STAIRS);
        placeDoor(helper, HOUSE_DOOR);
        helper.setBlock(HOUSE_CHEST, Blocks.CHEST);
    }

    /**
     * A bakery where {@link #buildCitizenHouse} builds: stone brick floor, plank walls, a roof of
     * brick stairs, and a chest and a smoker ({@link #BAKERY_SMOKER}) inside.
     */
    public static void buildBakery(GameTestHelper helper) {
        buildBox(helper, HOUSE_CORNER, Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.BRICK_STAIRS);
        placeDoor(helper, HOUSE_DOOR);
        helper.setBlock(HOUSE_CORNER.offset(1, 1, 1), Blocks.CHEST);
        helper.setBlock(BAKERY_SMOKER, Blocks.SMOKER);
    }

    /**
     * An empty 5x5 building with its floor's corner at {@code corner}: walls three blocks high on
     * the floor's edge and a roof on top, at {@code corner} + 4. Add its door and contents after.
     */
    public static void buildBox(GameTestHelper helper, BlockPos corner, Block floor, Block wall, Block roof) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                boolean edge = x == 0 || x == 4 || z == 0 || z == 4;
                helper.setBlock(corner.offset(x, 0, z), floor);
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(corner.offset(x, y, z), edge ? wall : Blocks.AIR);
                }
                helper.setBlock(corner.offset(x, 4, z), roof);
            }
        }
    }

    /** An oak door, both halves, with its lower half at {@code pos}. */
    public static void placeDoor(GameTestHelper helper, BlockPos pos) {
        BlockState door = Blocks.OAK_DOOR.defaultBlockState();
        helper.setBlock(pos, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(pos.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Detects a structure of the given type at a key block, without registering it. */
    public static StructureDetectionResult detect(GameTestHelper helper, ResourceLocation typeId, BlockPos keyBlock) {
        return detect(helper, typeId, keyBlock, UUID.randomUUID());
    }

    public static StructureDetectionResult detect(GameTestHelper helper, ResourceLocation typeId, BlockPos keyBlock,
                                                  UUID leaderId) {
        return StructureUtil.detect(helper.getLevel(), CftRegistry.getStructureType(typeId),
                helper.absolutePos(keyBlock), leaderId);
    }

    /** Asserts a structure is detected at a key block, and returns the result. */
    public static StructureDetectionResult assertDetected(GameTestHelper helper, ResourceLocation typeId, BlockPos keyBlock) {
        StructureDetectionResult result = detect(helper, typeId, keyBlock);
        if (!result.success()) {
            helper.fail("Expected " + typeId + " to be detected, but got " + result.reason().id(), keyBlock);
        }
        return result;
    }

    /** Asserts a structure isn't detected at a key block, for the expected reason. */
    public static void assertNotDetected(GameTestHelper helper, ResourceLocation typeId, BlockPos keyBlock,
                                         StructureDetectionReason expected) {
        StructureDetectionResult result = detect(helper, typeId, keyBlock);
        helper.assertFalse(result.success(), typeId + " was detected");
        helper.assertTrue(result.reason().equals(expected),
                "Expected " + expected.id() + ", got " + result.reason().id());
    }

    /** Detects a structure and registers it, like the leader staff does; fails the test if it isn't detected. */
    public static Structure register(GameTestHelper helper, ResourceLocation typeId, BlockPos keyBlock) {
        Structure structure = assertDetected(helper, typeId, keyBlock).structure();
        StructuresData.get(helper.getLevel()).addStructure(structure);
        return structure;
    }

    /**
     * A Xoonglin of the given class, set up like a spawned one but standing still: it has no AI,
     * so only the test acts on it.
     */
    public static XoonglinEntity spawnXoonglin(GameTestHelper helper, BlockPos pos, ResourceLocation classId) {
        return spawnXoonglin(helper, pos, classId, UUID.randomUUID());
    }

    public static XoonglinEntity spawnXoonglin(GameTestHelper helper, BlockPos pos, ResourceLocation classId,
                                               UUID leaderId) {
        XoonglinEntity xoonglin = helper.spawnWithNoFreeWill(CftEntities.XOONGLIN.get(), pos);
        XoonglinSpawner.setUpXoonglin(xoonglin, CftRegistry.SOCIAL_CLASSES.get(classId), leaderId);
        return xoonglin;
    }

    /**
     * A leader online in the test's level, which class changes require: a fake player added to the
     * level without logging in, so nothing tries to talk to its client. Remove it with
     * {@link #removeLeader} when the test is done.
     */
    public static ServerPlayer addOnlineLeader(GameTestHelper helper) {
        ServerPlayer leader = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "cft_test_leader"));
        helper.getLevel().addNewPlayer(leader);
        return leader;
    }

    public static void removeLeader(GameTestHelper helper, ServerPlayer leader) {
        helper.getLevel().removePlayerImmediately(leader, Entity.RemovalReason.DISCARDED);
    }

    /** Makes a Xoonglin a user of a registered structure and its home, as claiming it does. */
    public static void moveIn(XoonglinEntity xoonglin, Structure house) {
        house.addUser(xoonglin.getUUID());
        xoonglin.setHome(HouseStructure.of(house));
    }

    /**
     * A harmless need for a potato, read like a datapack's, with the given {@code active_when} JSON
     * array (see {@link #onlyIn} and {@link #inDimension}).
     */
    public static Need potatoNeed(String activeWhen) {
        String json = "{\"type\": \"cft:goods\", \"damage\": 0, \"damage_threshold\": 0, \"provided_happiness\": 5,"
                + " \"satisfaction_threshold\": 0.75, \"frequency\": 0.33, \"item\": {\"item\": \"minecraft:potato\"},"
                + " \"quantity\": 1, \"active_when\": " + activeWhen + "}";
        return Need.NEED_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    /** An {@code active_when} array for a need that only applies in the given dimension. */
    public static String onlyIn(String dimension) {
        return "[" + inDimension(dimension) + "]";
    }

    /** A {@code cft:dimension} condition, as JSON. */
    public static String inDimension(String dimension) {
        return "{\"type\": \"cft:dimension\", \"dimensions\": \"" + dimension + "\"}";
    }

    /** Gives a Xoonglin a satisfier in place of the one it has for the same need. */
    public static void replaceNeed(XoonglinEntity xoonglin, NeedSatisfier<? extends Need> satisfier) {
        List<NeedSatisfier<? extends Need>> needs = new ArrayList<>(xoonglin.getNeeds());
        needs.replaceAll(existing -> existing.getNeedId().equals(satisfier.getNeedId()) ? satisfier : existing);
        xoonglin.setNeeds(needs);
    }

    /** A fresh satisfier for a need, to check it on a Xoonglin whatever its class. */
    public static NeedSatisfier<? extends Need> satisfier(ResourceLocation needId) {
        return NeedSatisfierMapper.createNeedSatisfier(needId, CftRegistry.NEEDS.get(needId));
    }
}
