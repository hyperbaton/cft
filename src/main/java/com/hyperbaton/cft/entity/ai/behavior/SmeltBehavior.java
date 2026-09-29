package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.job.SmelterJob;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.FurnaceUtil;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Makes a smelter work at its workshop: it stands by the workshop's key block and every now and
 * then tends all the workshop's furnaces, collecting their results and loading them with things
 * to cook and fuel from the workshop's chests.
 */
public class SmeltBehavior extends Behavior<XoonglinEntity> {

    private static final int REPATH_INTERVAL = 40;
    /** Ticks between rounds of tending the furnaces. */
    private static final int TEND_INTERVAL = 100;

    private enum State {
        TRAVELING, TENDING
    }

    private State state;
    private BlockPos workshopKeyBlock;
    private int repathTimer;
    private int ticksUntilTending;

    public SmeltBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        SmelterJob job = getSmelterJob(entity);
        return job != null && entity.getAssignedStructurePos(job.getRequiredStructureType()) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        SmelterJob job = getSmelterJob(entity);
        workshopKeyBlock = entity.getAssignedStructurePos(job.getRequiredStructureType());
        state = State.TRAVELING;
        repathTimer = 0;
        ticksUntilTending = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_SMELT.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        SmelterJob job = getSmelterJob(entity);
        if (job == null || workshopKeyBlock == null) return;

        switch (state) {
            case TRAVELING -> tickTraveling(entity);
            case TENDING -> tickTending(level, entity, job);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
    }

    private void tickTraveling(XoonglinEntity entity) {
        if (isAtWorkshop(entity)) {
            entity.getNavigation().stop();
            state = State.TENDING;
            return;
        }
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            entity.getNavigation().moveTo(workshopKeyBlock.getX() + 0.5, workshopKeyBlock.getY(),
                    workshopKeyBlock.getZ() + 0.5, 1.0);
        }
    }

    private void tickTending(ServerLevel level, XoonglinEntity entity, SmelterJob job) {
        if (!isAtWorkshop(entity)) {
            state = State.TRAVELING;
            repathTimer = 0;
            return;
        }
        if (--ticksUntilTending > 0) return;
        ticksUntilTending = TEND_INTERVAL;

        Optional<Structure> workshop = findWorkshop(level, job);
        if (workshop.isEmpty()) return;

        List<Container> storage = FurnaceUtil.findStorage(level, workshop.get());
        boolean tended = false;
        for (AbstractFurnaceBlockEntity furnace : FurnaceUtil.findFurnaces(level, workshop.get())) {
            tended |= FurnaceUtil.tend(level, furnace, storage, job::accepts);
        }
        if (tended) {
            entity.swing(InteractionHand.MAIN_HAND);
        }
    }

    private boolean isAtWorkshop(XoonglinEntity entity) {
        return entity.position().distanceTo(Vec3.atCenterOf(workshopKeyBlock))
                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get();
    }

    private Optional<Structure> findWorkshop(ServerLevel level, SmelterJob job) {
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        return data.findByKeyBlock(workshopKeyBlock)
                .filter(structure -> structure.getStructureTypeId().equals(job.getRequiredStructureType()));
    }

    private SmelterJob getSmelterJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof SmelterJob smelterJob ? smelterJob : null;
    }
}
