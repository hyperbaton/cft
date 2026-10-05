package com.hyperbaton.cft.gametest;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureDetectionReason;
import com.hyperbaton.cft.structure.StructureDetectionReasons;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.util.GameTestUtil;
import com.hyperbaton.cft.util.StructureUtil;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.UUID;

import static com.hyperbaton.cft.util.GameTestUtil.*;

/** Detecting a house, why it fails when it's wrongly built, and detecting it again later. */
@GameTestHolder(CftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class StructureDetectionTests {
    private static final ResourceLocation SMELTERY = cftId("smeltery");

    @GameTest(template = EMPTY_TEMPLATE)
    public static void houseIsDetected(GameTestHelper helper) {
        buildCitizenHouse(helper);
        StructureDetectionResult result = assertDetected(helper, CITIZEN_HOUSE, HOUSE_DOOR);
        helper.assertTrue(result.structure().getStructureTypeId().equals(CITIZEN_HOUSE),
                "Detected as " + result.structure().getStructureTypeId());
        helper.assertTrue(result.structure().getKeyBlockPos().equals(helper.absolutePos(HOUSE_DOOR)),
                "The key block isn't the door");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void houseWithoutChestIsNotDetected(GameTestHelper helper) {
        buildCitizenHouse(helper);
        helper.setBlock(HOUSE_CHEST, Blocks.AIR);
        assertFails(helper, StructureDetectionReasons.INVALID_INTERIOR);
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void houseWithHoleInRoofIsNotDetected(GameTestHelper helper) {
        buildCitizenHouse(helper);
        helper.setBlock(HOUSE_CORNER.offset(2, 4, 2), Blocks.AIR);
        assertFails(helper, StructureDetectionReasons.INVALID_ROOF);
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void houseWithWrongFloorIsNotDetected(GameTestHelper helper) {
        buildCitizenHouse(helper);
        helper.setBlock(HOUSE_DOOR.below(), Blocks.DIRT);
        assertFails(helper, StructureDetectionReasons.NO_FLOOR);
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void houseWithWrongWallBlockIsNotDetected(GameTestHelper helper) {
        buildCitizenHouse(helper);
        helper.setBlock(HOUSE_WALL, Blocks.DIRT);
        StructureDetectionResult result = detect(helper, CITIZEN_HOUSE, HOUSE_DOOR);
        helper.assertFalse(result.success(), "A house with a dirt wall was detected");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void doorUpperHalfLeadsToKeyBlock(GameTestHelper helper) {
        buildCitizenHouse(helper);
        BlockPos upper = helper.absolutePos(HOUSE_DOOR.above());
        BlockPos keyBlock = StructureUtil.keyBlockPos(helper.getLevel().getBlockState(upper), upper);
        helper.assertTrue(keyBlock.equals(helper.absolutePos(HOUSE_DOOR)), "The upper half didn't lead to the lower one");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void recheckKeepsStandingStructure(GameTestHelper helper) {
        buildCitizenHouse(helper);
        Structure structure = register(helper, CITIZEN_HOUSE, HOUSE_DOOR);
        helper.assertTrue(StructureUtil.recheck(helper.getLevel(), structure).success(), "A standing house failed its recheck");
        helper.assertTrue(isRegistered(helper, HOUSE_DOOR), "A standing house was unregistered");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void structureIsNotRegisteredThroughEachOfItsKeyBlocks(GameTestHelper helper) {
        // A smeltery can have several furnaces, any of them its key block
        BlockPos firstFurnace = HOUSE_CORNER.offset(1, 1, 1);
        BlockPos secondFurnace = HOUSE_CORNER.offset(3, 1, 1);
        buildBox(helper, HOUSE_CORNER, Blocks.STONE_BRICKS, Blocks.COBBLESTONE, Blocks.STONE_BRICKS);
        placeDoor(helper, HOUSE_DOOR);
        helper.setBlock(HOUSE_CHEST, Blocks.CHEST);
        helper.setBlock(firstFurnace, Blocks.FURNACE);
        helper.setBlock(secondFurnace, Blocks.FURNACE);

        register(helper, SMELTERY, firstFurnace);
        assertNotDetected(helper, SMELTERY, secondFurnace, StructureDetectionReasons.OVERLAPPING_STRUCTURE);
        // Its own key block is still fine, as when it's checked again
        assertDetected(helper, SMELTERY, firstFurnace);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void structureHoldingAnotherKeyBlockIsNotDetected(GameTestHelper helper) {
        buildCitizenHouse(helper);
        // A structure registered with its key block in the house's wall, and no blocks of its own
        Structure other = new Structure(helper.absolutePos(HOUSE_WALL), 1, UUID.randomUUID(), BAKERY, 1, Map.of());
        StructuresData.get(helper.getLevel()).addStructure(other);
        StructureDetectionResult result = detect(helper, CITIZEN_HOUSE, HOUSE_DOOR);
        StructuresData.get(helper.getLevel()).removeStructure(other);
        helper.assertTrue(result.reason().equals(StructureDetectionReasons.OVERLAPPING_STRUCTURE),
                "Expected " + StructureDetectionReasons.OVERLAPPING_STRUCTURE.id() + ", got " + result.reason().id());
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void recheckUnregistersDemolishedStructure(GameTestHelper helper) {
        buildCitizenHouse(helper);
        Structure structure = register(helper, CITIZEN_HOUSE, HOUSE_DOOR);
        helper.setBlock(HOUSE_WALL, Blocks.AIR);
        helper.assertFalse(StructureUtil.recheck(helper.getLevel(), structure).success(), "A demolished house passed its recheck");
        helper.assertFalse(isRegistered(helper, HOUSE_DOOR), "A demolished house is still registered");
        helper.succeed();
    }

    private static void assertFails(GameTestHelper helper, StructureDetectionReason expected) {
        assertNotDetected(helper, CITIZEN_HOUSE, HOUSE_DOOR, expected);
        helper.succeed();
    }

    private static boolean isRegistered(GameTestHelper helper, BlockPos keyBlock) {
        return StructuresData.get(helper.getLevel()).findByKeyBlock(helper.absolutePos(keyBlock)).isPresent();
    }
}
