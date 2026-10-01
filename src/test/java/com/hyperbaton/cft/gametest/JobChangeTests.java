package com.hyperbaton.cft.gametest;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.api.event.JobChangeEvent;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.JobUtil;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.hyperbaton.cft.util.GameTestUtil.*;

/** Changing a Xoonglin's job, and a job checking its workplace before it starts working there. */
@GameTestHolder(CftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JobChangeTests {
    private static final ResourceLocation BAKER_JOB = cftId("baker_job");
    private static final ResourceLocation FISHER_JOB = cftId("fisher_job");
    private static final ResourceLocation QUARRY_MINER_JOB = cftId("quarry_miner_job");
    private static final ResourceLocation QUARRY = cftId("quarry");
    private static final ResourceLocation MOUNTAIN_DWELLER = cftId("mountain_dweller");
    private static final BlockPos XOONGLIN_POS = new BlockPos(8, 1, 8);

    @GameTest(template = EMPTY_TEMPLATE)
    public static void playerChangesJob(GameTestHelper helper) {
        XoonglinEntity citizen = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        citizen.setJob(BAKER_JOB);

        helper.assertTrue(citizen.changeJob(FISHER_JOB, JobChangeEvent.Cause.PLAYER), "The job didn't change");
        helper.assertTrue(FISHER_JOB.equals(citizen.getJob()), "The job is " + citizen.getJob());
        helper.assertFalse(citizen.changeJob(FISHER_JOB, JobChangeEvent.Cause.PLAYER), "Changing to the same job counted as a change");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void cancelledJobChangeKeepsJob(GameTestHelper helper) {
        XoonglinEntity citizen = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        citizen.setJob(BAKER_JOB);
        Consumer<JobChangeEvent.Pre> listener = event -> {
            if (event.getXoonglin() == citizen) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, JobChangeEvent.Pre.class, listener);
        boolean changed = citizen.changeJob(FISHER_JOB, JobChangeEvent.Cause.PLAYER);
        NeoForge.EVENT_BUS.unregister(listener);

        helper.assertFalse(changed, "The cancelled change was reported as done");
        helper.assertTrue(BAKER_JOB.equals(citizen.getJob()), "The cancelled change went through");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void jobChangeLeavesWorkplace(GameTestHelper helper) {
        XoonglinEntity baker = spawnBakerAtWork(helper);
        Structure bakery = StructuresData.get(helper.getLevel()).findByKeyBlock(helper.absolutePos(BAKERY_SMOKER)).orElseThrow();

        helper.assertTrue(baker.changeJob(FISHER_JOB, JobChangeEvent.Cause.PLAYER), "The job didn't change");
        helper.assertTrue(baker.getAssignedStructurePos(BAKERY) == null, "It's still assigned to the bakery");
        helper.assertFalse(bakery.isUser(baker.getUUID()), "It's still a user of the bakery");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void workplaceCheckKeepsStandingWorkplace(GameTestHelper helper) {
        XoonglinEntity baker = spawnBakerAtWork(helper);

        helper.assertTrue(JobUtil.checkWorkplace(baker, CftRegistry.JOBS.get(BAKER_JOB)), "A standing bakery failed the check");
        helper.assertTrue(baker.getAssignedStructurePos(BAKERY) != null, "It gave up a standing bakery");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void workplaceCheckDropsDemolishedWorkplace(GameTestHelper helper) {
        XoonglinEntity baker = spawnBakerAtWork(helper);
        helper.setBlock(HOUSE_WALL, Blocks.AIR);

        helper.assertFalse(JobUtil.checkWorkplace(baker, CftRegistry.JOBS.get(BAKER_JOB)), "A demolished bakery passed the check");
        helper.assertTrue(baker.getAssignedStructurePos(BAKERY) == null, "It kept a demolished bakery");
        helper.assertFalse(StructuresData.get(helper.getLevel()).findByKeyBlock(helper.absolutePos(BAKERY_SMOKER)).isPresent(),
                "The demolished bakery is still registered");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void quarryIsNotRecheckedWhileMined(GameTestHelper helper) {
        // A quarry is registered without being built: it's only looked up, never detected again
        BlockPos quarryKey = helper.absolutePos(new BlockPos(1, 1, 1));
        Structure quarry = new Structure(quarryKey, 1, UUID.randomUUID(), QUARRY, 1, Map.of());
        StructuresData.get(helper.getLevel()).addStructure(quarry);
        XoonglinEntity miner = spawnXoonglin(helper, XOONGLIN_POS, MOUNTAIN_DWELLER);
        miner.setJob(QUARRY_MINER_JOB);
        miner.assignStructure(QUARRY, quarryKey);

        boolean kept = JobUtil.checkWorkplace(miner, CftRegistry.JOBS.get(QUARRY_MINER_JOB));
        StructuresData.get(helper.getLevel()).removeStructure(quarry);
        helper.assertTrue(kept, "The quarry was detected again while mined");
        helper.assertTrue(quarryKey.equals(miner.getAssignedStructurePos(QUARRY)), "The miner gave up its quarry");
        helper.succeed();
    }

    /** A baker assigned to a registered bakery, and one of its users. */
    private static XoonglinEntity spawnBakerAtWork(GameTestHelper helper) {
        buildBakery(helper);
        Structure bakery = register(helper, BAKERY, BAKERY_SMOKER);
        XoonglinEntity baker = spawnXoonglin(helper, XOONGLIN_POS, CITIZEN);
        baker.setJob(BAKER_JOB);
        baker.assignStructure(BAKERY, bakery.getKeyBlockPos());
        bakery.addUser(baker.getUUID());
        return baker;
    }
}
