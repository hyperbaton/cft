package com.hyperbaton.cft.gametest;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.need.condition.AnyOfCondition;
import com.hyperbaton.cft.need.condition.BiomeCondition;
import com.hyperbaton.cft.need.condition.DimensionCondition;
import com.hyperbaton.cft.need.condition.NeedCondition;
import com.hyperbaton.cft.need.condition.NotCondition;
import com.hyperbaton.cft.need.condition.TimeCondition;
import com.hyperbaton.cft.need.condition.WeatherCondition;
import com.hyperbaton.cft.need.condition.WeatherCondition.Weather;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfierMapper;
import com.hyperbaton.cft.util.RegistryEntries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.hyperbaton.cft.util.GameTestUtil.*;

/**
 * The conditions of a need's {@code active_when}, tested against the world as it is (the time,
 * weather and biome of the test world aren't fixed), and the needs check leaving alone the needs
 * whose conditions don't hold.
 */
@GameTestHolder(CftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class NeedConditionTests {
    private static final BlockPos XOONGLIN_POS = new BlockPos(4, 1, 4);
    private static final NeedCondition IN_OVERWORLD = new DimensionCondition(List.of(Level.OVERWORLD));
    private static final NeedCondition IN_NETHER = new DimensionCondition(List.of(Level.NETHER));

    @GameTest(template = EMPTY_TEMPLATE)
    public static void timeCondition(GameTestHelper helper) {
        XoonglinEntity xoonglin = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        int now = (int) (helper.getLevel().getDayTime() % 24000L);

        // Ranges around now hold whether or not they go past midnight
        assertHolds(helper, xoonglin, new TimeCondition((now + 12000) % 24000, (now + 1) % 24000), true);
        assertHolds(helper, xoonglin, new TimeCondition((now + 1) % 24000, (now + 1000) % 24000), false);
        assertHolds(helper, xoonglin, new TimeCondition(12000, 0), now >= 12000);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void weatherCondition(GameTestHelper helper) {
        XoonglinEntity xoonglin = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        Weather now = Weather.of(helper.getLevel());
        List<Weather> others = new ArrayList<>(Arrays.asList(Weather.values()));
        others.remove(now);

        assertHolds(helper, xoonglin, new WeatherCondition(List.of(now)), true);
        assertHolds(helper, xoonglin, new WeatherCondition(others), false);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void dimensionCondition(GameTestHelper helper) {
        XoonglinEntity xoonglin = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        assertHolds(helper, xoonglin, IN_OVERWORLD, true);
        assertHolds(helper, xoonglin, IN_NETHER, false);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void biomeCondition(GameTestHelper helper) {
        XoonglinEntity xoonglin = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        ResourceLocation biome = helper.getLevel().getBiome(xoonglin.getOnPos()).unwrapKey().orElseThrow().location();

        assertHolds(helper, xoonglin, new BiomeCondition(RegistryEntries.of(biome)), true);
        assertHolds(helper, xoonglin, new BiomeCondition(RegistryEntries.empty()), false);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void anyOfAndNotConditions(GameTestHelper helper) {
        XoonglinEntity xoonglin = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        assertHolds(helper, xoonglin, new AnyOfCondition(List.of(IN_NETHER, IN_OVERWORLD)), true);
        assertHolds(helper, xoonglin, new AnyOfCondition(List.of(IN_NETHER)), false);
        assertHolds(helper, xoonglin, new NotCondition(IN_NETHER), true);
        assertHolds(helper, xoonglin, new NotCondition(IN_OVERWORLD), false);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void needIsActiveWhenAllConditionsHold(GameTestHelper helper) {
        XoonglinEntity xoonglin = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        helper.assertTrue(potatoNeed("[]").isActive(xoonglin), "A need without conditions isn't active");
        helper.assertTrue(potatoNeed(onlyIn("minecraft:overworld")).isActive(xoonglin), "A need for the overworld isn't active there");
        helper.assertFalse(potatoNeed("[" + inDimension("minecraft:overworld") + ", " + inDimension("minecraft:the_nether") + "]")
                .isActive(xoonglin), "A need was active with one of its conditions failing");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void inactiveNeedIsNotChecked(GameTestHelper helper) {
        XoonglinEntity xoonglin = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        NeedSatisfier<? extends Need> inactive = NeedSatisfierMapper.createNeedSatisfier(cftId("test_nether_potato"),
                potatoNeed(onlyIn("minecraft:the_nether")));
        NeedSatisfier<? extends Need> active = NeedSatisfierMapper.createNeedSatisfier(cftId("test_overworld_potato"),
                potatoNeed(onlyIn("minecraft:overworld")));
        double before = inactive.getSatisfaction();
        xoonglin.setNeeds(new ArrayList<>(List.of(inactive, active)));

        // Once the active one has been checked, the inactive one has been skipped
        helper.succeedWhen(() -> {
            helper.assertTrue(active.getSatisfaction() < before, "The active need wasn't checked yet");
            helper.assertFalse(inactive.isActive(), "The need for the nether is active in the overworld");
            helper.assertTrue(inactive.getSatisfaction() == before, "The inactive need wore off");
            helper.assertFalse(xoonglin.getUnsatisfiedVisibleNeeds().contains(inactive), "The inactive need is reported as unsatisfied");
        });
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void conditionsAreSavedWithTheNeed(GameTestHelper helper) {
        Need need = potatoNeed("[{\"type\": \"cft:not\", \"condition\": {\"type\": \"cft:any_of\", \"conditions\": ["
                + "{\"type\": \"cft:time\", \"from\": 12000, \"to\": 0}, {\"type\": \"cft:weather\", \"weather\": \"rain\"},"
                + "{\"type\": \"cft:biome\", \"biomes\": \"#minecraft:is_ocean\"}]}}]");
        Need saved = Need.NEED_CODEC.parse(NbtOps.INSTANCE, Need.NEED_CODEC.encodeStart(NbtOps.INSTANCE, need).getOrThrow())
                .getOrThrow();
        helper.assertTrue(saved.getProperties().activeWhen().equals(need.getProperties().activeWhen()),
                "The conditions changed when saved and read back");
        helper.succeed();
    }

    private static void assertHolds(GameTestHelper helper, XoonglinEntity xoonglin, NeedCondition condition, boolean expected) {
        helper.assertTrue(condition.test(xoonglin) == expected, condition + " should " + (expected ? "" : "not ") + "hold");
    }
}
