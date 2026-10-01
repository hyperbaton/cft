package com.hyperbaton.cft.gametest;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.GoodsNeed;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.hyperbaton.cft.util.GameTestUtil.*;

/** One check of a need on a Xoonglin, through its satisfier: what it takes and what it leaves behind. */
@GameTestHolder(CftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class NeedTests {
    private static final ResourceLocation BREAD_NEED = cftId("bread_need");
    private static final ResourceLocation CITIZEN_HOME = cftId("citizen_home");
    /** Outside the house, where the Xoonglin stands. */
    private static final BlockPos XOONGLIN_POS = new BlockPos(8, 1, 8);

    @GameTest(template = EMPTY_TEMPLATE)
    public static void goodsNeedConsumesFromInventory(GameTestHelper helper) {
        XoonglinEntity xoonglin = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        xoonglin.getInventory().addItem(new ItemStack(Items.BREAD, 2));
        NeedSatisfier<? extends Need> satisfier = satisfier(BREAD_NEED);

        helper.assertTrue(satisfier.satisfy(xoonglin), "Bread in the inventory didn't satisfy the need");
        satisfier.updateSatisfied(xoonglin);
        helper.assertTrue(xoonglin.getInventory().countItem(Items.BREAD) == 0, "The bread wasn't eaten");
        helper.assertTrue(satisfier.getSatisfaction() == 1.0, "Satisfaction is " + satisfier.getSatisfaction());
        helper.assertTrue(satisfier.isSatisfied(), "The need isn't marked as satisfied");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void goodsNeedSeeksSuppliesAtHome(GameTestHelper helper) {
        buildCitizenHouse(helper);
        if (helper.getBlockEntity(HOUSE_CHEST) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.BREAD, 4));
        }
        XoonglinEntity xoonglin = spawnXoonglinAtHome(helper);
        NeedSatisfier<? extends Need> satisfier = satisfier(BREAD_NEED);
        double before = satisfier.getSatisfaction();

        helper.assertFalse(satisfier.satisfy(xoonglin), "The need was satisfied with an empty inventory");
        helper.assertTrue(satisfier.getSatisfaction() < before, "The need didn't wear off");
        helper.assertTrue(xoonglin.getBrain().getMemory(CftMemoryModuleType.HOME_CONTAINER.get())
                        .filter(helper.absolutePos(HOUSE_CHEST)::equals).isPresent(),
                "The Xoonglin wasn't sent to the chest with bread");
        helper.assertTrue(xoonglin.getBrain().getMemory(CftMemoryModuleType.SUPPLIES_NEEDED.get())
                        .filter(ingredients -> ingredients.contains(((GoodsNeed) satisfier.getNeed()).getIngredient()))
                        .isPresent(),
                "Bread isn't among the supplies needed");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void homeNeedKeepsStandingHome(GameTestHelper helper) {
        buildCitizenHouse(helper);
        XoonglinEntity xoonglin = spawnXoonglinAtHome(helper);

        helper.assertTrue(satisfier(CITIZEN_HOME).satisfy(xoonglin), "A standing home didn't satisfy the need");
        helper.assertTrue(xoonglin.getHome() != null, "The Xoonglin lost a standing home");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void homeNeedLosesDemolishedHome(GameTestHelper helper) {
        buildCitizenHouse(helper);
        XoonglinEntity xoonglin = spawnXoonglinAtHome(helper);
        helper.setBlock(HOUSE_WALL, Blocks.AIR);

        helper.assertFalse(satisfier(CITIZEN_HOME).satisfy(xoonglin), "A demolished home satisfied the need");
        helper.assertTrue(xoonglin.getHome() == null, "The Xoonglin kept a demolished home");
        helper.assertTrue(xoonglin.getBrain().getMemory(CftMemoryModuleType.HOME_NEEDED.get()).orElse(false),
                "The Xoonglin isn't looking for a new home");
        helper.assertFalse(StructuresData.get(helper.getLevel()).findByKeyBlock(helper.absolutePos(HOUSE_DOOR)).isPresent(),
                "The demolished home is still registered");
        helper.succeed();
    }

    /** A citizen living in the house built with {@code buildCitizenHouse}, registered first. */
    private static XoonglinEntity spawnXoonglinAtHome(GameTestHelper helper) {
        Structure house = register(helper, CITIZEN_HOUSE, HOUSE_DOOR);
        XoonglinEntity xoonglin = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        moveIn(xoonglin, house);
        return xoonglin;
    }
}
