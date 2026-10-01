package com.hyperbaton.cft.gametest;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureDetectionReasons;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.hyperbaton.cft.util.GameTestUtil.*;

/**
 * One kind of structure per detector, built as CFT's own structure of that kind: enclosed
 * buildings (with a key block inside, and with lighting), multi-storey buildings, open-air
 * platforms, pastures, monuments and compounds.
 */
@GameTestHolder(CftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class StructureTypeTests {
    private static final ResourceLocation NOBLE_HOUSE = cftId("noble_house");
    private static final ResourceLocation TWO_STOREY_HOUSE = cftId("two_storey_house");
    private static final ResourceLocation FARM = cftId("farm");
    private static final ResourceLocation PASTURE = cftId("pasture");
    private static final ResourceLocation OBELISK = cftId("obelisk");
    private static final ResourceLocation VILLAGE_SQUARE = cftId("village_square");

    /** The corner of the ground of platforms and pastures, a 7x7 ring with a 5x5 surface inside. */
    private static final BlockPos PLATFORM = new BlockPos(1, 1, 1);
    /** The key block of platforms and pastures, in the middle of the border's north side. */
    private static final BlockPos PLATFORM_KEY = PLATFORM.offset(3, 1, 0);
    private static final BlockPos OBELISK_BASE = new BlockPos(4, 1, 4);
    private static final BlockPos BELL = new BlockPos(4, 2, 4);

    // ---- Enclosed buildings ----

    @GameTest(template = EMPTY_TEMPLATE)
    public static void bakeryIsDetectedFromItsSmoker(GameTestHelper helper) {
        buildBakery(helper);
        assertDetected(helper, BAKERY, BAKERY_SMOKER);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void darkNobleHouseIsNotDetected(GameTestHelper helper) {
        buildNobleHouse(helper);
        assertNotDetected(helper, NOBLE_HOUSE, HOUSE_DOOR, StructureDetectionReasons.TOO_DARK);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void litNobleHouseIsDetected(GameTestHelper helper) {
        buildNobleHouse(helper);
        // Shines in through a window; light spreads over the next ticks
        helper.setBlock(HOUSE_CORNER.offset(-1, 1, 2), Blocks.GLOWSTONE);
        helper.succeedWhen(() -> assertDetected(helper, NOBLE_HOUSE, HOUSE_DOOR));
    }

    // ---- Multi-storey buildings ----

    @GameTest(template = EMPTY_TEMPLATE)
    public static void twoStoreyHouseIsDetected(GameTestHelper helper) {
        buildTwoStoreyHouse(helper, true);
        assertDetected(helper, TWO_STOREY_HOUSE, HOUSE_DOOR);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void oneStoreyIsNotEnough(GameTestHelper helper) {
        buildTwoStoreyHouse(helper, false);
        assertNotDetected(helper, TWO_STOREY_HOUSE, HOUSE_DOOR, StructureDetectionReasons.NOT_ENOUGH_STOREYS);
        helper.succeed();
    }

    // ---- Open-air platforms ----

    @GameTest(template = EMPTY_TEMPLATE)
    public static void farmIsDetected(GameTestHelper helper) {
        buildFarm(helper);
        assertDetected(helper, FARM, PLATFORM_KEY);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void farmWithGapInFenceIsNotDetected(GameTestHelper helper) {
        buildFarm(helper);
        helper.setBlock(PLATFORM.offset(0, 1, 3), Blocks.AIR);
        assertNotDetected(helper, FARM, PLATFORM_KEY, StructureDetectionReasons.BORDER_NOT_CLOSED);
        helper.succeed();
    }

    // ---- Pastures ----

    @GameTest(template = EMPTY_TEMPLATE)
    public static void pastureWithAnimalsIsDetected(GameTestHelper helper) {
        buildPasture(helper, 3);
        assertDetected(helper, PASTURE, PLATFORM_KEY);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void pastureWithTooFewAnimalsIsNotDetected(GameTestHelper helper) {
        buildPasture(helper, 2);
        assertNotDetected(helper, PASTURE, PLATFORM_KEY, StructureDetectionReasons.NOT_ENOUGH_ANIMALS);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void pastureIsNotDetectedTwice(GameTestHelper helper) {
        buildPasture(helper, 3);
        register(helper, PASTURE, PLATFORM_KEY);
        // Another hay bale of the same pen
        assertNotDetected(helper, PASTURE, PLATFORM.offset(0, 1, 3), StructureDetectionReasons.OVERLAPPING_STRUCTURE);
        helper.succeed();
    }

    // ---- Monuments ----

    @GameTest(template = EMPTY_TEMPLATE)
    public static void obeliskIsDetected(GameTestHelper helper) {
        buildObelisk(helper, 5);
        assertDetected(helper, OBELISK, OBELISK_BASE);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void shortObeliskIsNotDetected(GameTestHelper helper) {
        buildObelisk(helper, 3);
        assertNotDetected(helper, OBELISK, OBELISK_BASE, StructureDetectionReasons.MONUMENT_TOO_SHORT);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void unevenObeliskIsNotDetected(GameTestHelper helper) {
        buildObelisk(helper, 5);
        helper.setBlock(OBELISK_BASE.offset(1, 2, 0), Blocks.QUARTZ_BLOCK);
        assertNotDetected(helper, OBELISK, OBELISK_BASE, StructureDetectionReasons.LAYERS_NOT_IDENTICAL);
        helper.succeed();
    }

    // ---- Compounds ----

    @GameTest(template = EMPTY_TEMPLATE)
    public static void villageSquareWithHousesIsDetected(GameTestHelper helper) {
        buildVillageSquare(helper);
        UUID leaderId = UUID.randomUUID();
        List<Structure> houses = registerFakeSettlerHouses(helper, leaderId);
        boolean detected = detect(helper, VILLAGE_SQUARE, BELL, leaderId).success();
        houses.forEach(StructuresData.get(helper.getLevel())::removeStructure);
        helper.assertTrue(detected, "The village square wasn't detected with three houses around");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void villageSquareWithoutHousesIsNotDetected(GameTestHelper helper) {
        buildVillageSquare(helper);
        assertNotDetected(helper, VILLAGE_SQUARE, BELL, StructureDetectionReasons.MISSING_REQUIRED_STRUCTURES);
        helper.succeed();
    }

    // ---- Builds ----

    /** Quartz walls with four glass panes (a valid share), and no light inside. */
    private static void buildNobleHouse(GameTestHelper helper) {
        buildBox(helper, HOUSE_CORNER, Blocks.POLISHED_DIORITE, Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_QUARTZ);
        placeDoor(helper, HOUSE_DOOR);
        helper.setBlock(HOUSE_CHEST, Blocks.CHEST);
        for (BlockPos pane : List.of(new BlockPos(0, 1, 2), new BlockPos(4, 1, 2),
                new BlockPos(2, 1, 4), new BlockPos(2, 2, 4))) {
            helper.setBlock(HOUSE_CORNER.offset(pane), Blocks.GLASS_PANE);
        }
    }

    /**
     * Plank floors, log walls and a ladder going up from the first storey through the ceiling,
     * which is the second storey's floor. Without the second storey, only the first one is built.
     */
    private static void buildTwoStoreyHouse(GameTestHelper helper, boolean secondStorey) {
        buildBox(helper, HOUSE_CORNER, Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.OAK_PLANKS);
        if (secondStorey) {
            buildBox(helper, HOUSE_CORNER.above(4), Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.OAK_PLANKS);
        }
        placeDoor(helper, HOUSE_DOOR);
        helper.setBlock(HOUSE_CHEST, Blocks.CHEST);
        for (int y = 1; y <= 4; y++) {
            // Against the west wall
            helper.setBlock(HOUSE_CORNER.offset(1, y, 2),
                    Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        }
    }

    /**
     * The ground of a platform: a 7x7 square on stone, its outer ring of dirt and a 5x5 surface
     * inside, with the border's ring of blocks above the dirt.
     */
    private static void buildPlatform(GameTestHelper helper, Block surface, Block border) {
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                boolean edge = x == 0 || x == 6 || z == 0 || z == 6;
                helper.setBlock(PLATFORM.offset(x, -1, z), Blocks.STONE);
                helper.setBlock(PLATFORM.offset(x, 0, z), edge ? Blocks.DIRT : surface);
                if (edge) helper.setBlock(PLATFORM.offset(x, 1, z), border);
            }
        }
    }

    /** Farmland watered from the middle, fenced, with a fence gate and a chest in the fence. */
    private static void buildFarm(GameTestHelper helper) {
        buildPlatform(helper, Blocks.FARMLAND, Blocks.OAK_FENCE);
        helper.setBlock(PLATFORM.offset(3, 0, 3), Blocks.WATER);
        helper.setBlock(PLATFORM_KEY, Blocks.OAK_FENCE_GATE);
        helper.setBlock(PLATFORM.offset(6, 1, 3), Blocks.CHEST);
    }

    /** Grass fenced two blocks high, with four hay bales and a chest in the fence, and some cows. */
    private static void buildPasture(GameTestHelper helper, int cows) {
        buildPlatform(helper, Blocks.GRASS_BLOCK, Blocks.OAK_FENCE);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                if (x == 0 || x == 6 || z == 0 || z == 6) helper.setBlock(PLATFORM.offset(x, 2, z), Blocks.OAK_FENCE);
            }
        }
        for (BlockPos hay : List.of(PLATFORM_KEY, PLATFORM.offset(0, 1, 3), PLATFORM.offset(6, 1, 3),
                PLATFORM.offset(3, 1, 6))) {
            helper.setBlock(hay, Blocks.HAY_BLOCK);
        }
        helper.setBlock(PLATFORM.offset(1, 1, 6), Blocks.CHEST);
        for (int i = 0; i < cows; i++) {
            helper.spawnWithNoFreeWill(EntityType.COW, PLATFORM.offset(2 + i, 1, 2 + i));
        }
    }

    /** A chiseled quartz base under a column of quartz blocks. */
    private static void buildObelisk(GameTestHelper helper, int height) {
        helper.setBlock(OBELISK_BASE, Blocks.CHISELED_QUARTZ_BLOCK);
        for (int y = 1; y <= height; y++) {
            helper.setBlock(OBELISK_BASE.above(y), Blocks.QUARTZ_BLOCK);
        }
    }

    /** A 5x5 stone brick square with the bell in the middle. */
    private static void buildVillageSquare(GameTestHelper helper) {
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE_BRICKS);
            }
        }
        helper.setBlock(BELL, Blocks.BELL);
    }

    /**
     * Three settler houses of a leader around the square, registered without being built: the
     * square only looks them up among the registered structures. The test removes them after.
     */
    private static List<Structure> registerFakeSettlerHouses(GameTestHelper helper, UUID leaderId) {
        List<Structure> houses = List.of(new BlockPos(0, 1, 0), new BlockPos(8, 1, 0), new BlockPos(0, 1, 8)).stream()
                .map(pos -> new Structure(helper.absolutePos(pos), 1, leaderId, SETTLER_HOUSE, 1, Map.of()))
                .toList();
        houses.forEach(StructuresData.get(helper.getLevel())::addStructure);
        return houses;
    }
}
