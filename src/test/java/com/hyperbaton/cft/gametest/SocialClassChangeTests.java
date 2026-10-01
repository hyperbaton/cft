package com.hyperbaton.cft.gametest;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.api.event.SocialClassChangeEvent;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.structure.Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.hyperbaton.cft.util.GameTestUtil.*;

/**
 * Xoonglins moving up and down social classes as they check their needs, with their leader
 * online, and what listeners of {@link SocialClassChangeEvent.Pre} can do about it.
 *
 * <p>A settler upgrades to citizen when it's happy enough (over 20), its potato need is
 * satisfied enough (a fresh need is) and settlers would still be over 30% of its leader's
 * Xoonglins; a citizen downgrades to settler when settlers are under 30%.
 */
@GameTestHolder(CftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class SocialClassChangeTests {
    private static final ResourceLocation MOUNTAIN_DWELLER = cftId("mountain_dweller");
    private static final BlockPos UPGRADING_POS = new BlockPos(8, 1, 8);
    private static final BlockPos STAYING_POS = new BlockPos(8, 1, 1);
    /** Long enough for a few needs checks, which happen once per second. */
    private static final int A_FEW_CHECKS = 70;

    @GameTest(template = EMPTY_TEMPLATE)
    public static void settlerUpgradesToCitizen(GameTestHelper helper) {
        buildSettlerHouse(helper);
        Structure house = register(helper, SETTLER_HOUSE, HOUSE_DOOR);
        ServerPlayer leader = addOnlineLeader(helper);
        XoonglinEntity settler = spawnUpgradingSettler(helper, leader);
        moveIn(settler, house);

        helper.succeedWhen(() -> {
            helper.assertTrue(CITIZEN.equals(settler.getSocialClassId()), "Still a " + settler.getSocialClassId());
            helper.assertTrue(settler.getNeeds().stream().map(NeedSatisfier::getNeedId).anyMatch(cftId("citizen_home")::equals),
                    "It didn't take the citizen's needs");
            helper.assertTrue(CftRegistry.SOCIAL_CLASSES.get(CITIZEN).getJobs().contains(settler.getJob()),
                    "Its job " + settler.getJob() + " isn't a citizen's job");
            // A settler's home isn't a citizen's
            helper.assertTrue(settler.getHome() == null, "It kept its settler home");
            helper.assertFalse(house.isUser(settler.getUUID()), "It's still a user of its settler home");
            removeLeader(helper, leader);
        });
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void citizenWithoutSettlersDowngrades(GameTestHelper helper) {
        ServerPlayer leader = addOnlineLeader(helper);
        XoonglinEntity citizen = spawnXoonglin(helper, UPGRADING_POS, CITIZEN, leader.getUUID());
        citizen.setHappiness(50);

        helper.succeedWhen(() -> {
            helper.assertTrue(SETTLER.equals(citizen.getSocialClassId()),
                    "A citizen with no settlers around is still a " + citizen.getSocialClassId());
            removeLeader(helper, leader);
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = A_FEW_CHECKS + 20)
    public static void cancelledUpgradeKeepsClass(GameTestHelper helper) {
        ServerPlayer leader = addOnlineLeader(helper);
        XoonglinEntity settler = spawnUpgradingSettler(helper, leader);
        AtomicInteger attempts = new AtomicInteger();
        Consumer<SocialClassChangeEvent.Pre> listener = listen(event -> {
            if (event.getXoonglin() == settler) {
                attempts.incrementAndGet();
                event.setCanceled(true);
            }
        });

        helper.runAfterDelay(A_FEW_CHECKS, () -> {
            NeoForge.EVENT_BUS.unregister(listener);
            removeLeader(helper, leader);
            helper.assertTrue(attempts.get() > 0, "The settler never tried to upgrade");
            helper.assertTrue(SETTLER.equals(settler.getSocialClassId()), "The cancelled upgrade went through");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void redirectedUpgradeChangesTarget(GameTestHelper helper) {
        ServerPlayer leader = addOnlineLeader(helper);
        XoonglinEntity settler = spawnUpgradingSettler(helper, leader);
        Consumer<SocialClassChangeEvent.Pre> listener = listen(event -> {
            if (event.getXoonglin() == settler) event.setNextClass(MOUNTAIN_DWELLER);
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(MOUNTAIN_DWELLER.equals(settler.getSocialClassId()), "It's a " + settler.getSocialClassId());
            NeoForge.EVENT_BUS.unregister(listener);
            removeLeader(helper, leader);
        });
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void inactiveNeedDoesNotBlockUpgrade(GameTestHelper helper) {
        ServerPlayer leader = addOnlineLeader(helper);
        XoonglinEntity settler = spawnUpgradingSettler(helper, leader);
        // The potato need an upgrade requires, unsatisfied, but only applying in the nether
        replaceNeed(settler, unsatisfiedPotatoNeed(onlyIn("minecraft:the_nether")));

        helper.succeedWhen(() -> {
            helper.assertTrue(CITIZEN.equals(settler.getSocialClassId()),
                    "An unsatisfied need that doesn't apply stopped the upgrade");
            removeLeader(helper, leader);
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = A_FEW_CHECKS + 20)
    public static void unsatisfiedNeedBlocksUpgrade(GameTestHelper helper) {
        ServerPlayer leader = addOnlineLeader(helper);
        XoonglinEntity settler = spawnUpgradingSettler(helper, leader);
        replaceNeed(settler, unsatisfiedPotatoNeed(onlyIn("minecraft:overworld")));

        helper.runAfterDelay(A_FEW_CHECKS, () -> {
            removeLeader(helper, leader);
            helper.assertTrue(SETTLER.equals(settler.getSocialClassId()), "It upgraded with its potato need unsatisfied");
            helper.succeed();
        });
    }

    /** The settler's potato need, the one its upgrade requires, with no satisfaction at all. */
    private static NeedSatisfier<? extends Need> unsatisfiedPotatoNeed(String activeWhen) {
        NeedSatisfier<? extends Need> satisfier = potatoNeed(activeWhen).createSatisfier(0.0, false);
        satisfier.setNeedId(cftId("potato_need"));
        return satisfier;
    }

    /**
     * A settler happy enough to upgrade, with an online leader and another, unhappy settler of
     * the same leader so that settlers stay over 30% after the upgrade.
     */
    private static XoonglinEntity spawnUpgradingSettler(GameTestHelper helper, ServerPlayer leader) {
        XoonglinEntity settler = spawnXoonglin(helper, UPGRADING_POS, SETTLER, leader.getUUID());
        settler.setHappiness(50);
        spawnXoonglin(helper, STAYING_POS, SETTLER, leader.getUUID()).setHappiness(0);
        return settler;
    }

    /** Listens to class changes until the test unregisters the listener. */
    private static Consumer<SocialClassChangeEvent.Pre> listen(Consumer<SocialClassChangeEvent.Pre> listener) {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, SocialClassChangeEvent.Pre.class, listener);
        return listener;
    }
}
