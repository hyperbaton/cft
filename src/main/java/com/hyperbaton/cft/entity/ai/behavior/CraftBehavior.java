package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.CrafterJob;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;

/**
 * Makes a crafter work at its workshop: it stands by the workshop's key block and, as
 * long as the workshop container holds the ingredients, repeatedly crafts the output
 * into that same container. If ingredients are missing, it waits by the workshop and
 * rechecks periodically.
 */
public class CraftBehavior extends JobBehavior<CrafterJob> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REPATH_INTERVAL = 40;
    // Ticks to wait before re-checking the container when ingredients are missing
    private static final int INGREDIENT_RETRY_COOLDOWN = 600;

    private enum State {
        TRAVELING(WorkStep.GOING_TO_WORK),
        CRAFTING(WorkStep.of("crafting"));

        /** Shown in the job tab while the behavior is in this state. */
        private final WorkStep step;

        State(WorkStep step) {
            this.step = step;
        }
    }

    private State state;
    private BlockPos workshopKeyBlock;
    private int repathTimer;
    private int waitTicks;
    private int craftProgress;

    public CraftBehavior() {
        super(CftMemoryModuleType.MUST_CRAFT.get(), CrafterJob.class, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        CrafterJob job = getJob(entity);
        return job != null && entity.getAssignedStructurePos(job.getRequiredStructureType()) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        CrafterJob job = getJob(entity);
        workshopKeyBlock = entity.getAssignedStructurePos(job.getRequiredStructureType());
        state = State.TRAVELING;
        repathTimer = 0;
        waitTicks = 0;
        craftProgress = 0;
    }

    @Override
    protected WorkStep workStep() {
        return state != null ? state.step : null;
    }

    @Override
    protected void tickWork(ServerLevel level, XoonglinEntity entity, long gameTime) {
        CrafterJob job = getJob(entity);
        if (job == null || workshopKeyBlock == null) return;

        switch (state) {
            case TRAVELING -> tickTraveling(entity);
            case CRAFTING -> tickCrafting(level, entity, job);
        }
    }

    @Override
    protected void stopWork(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
        craftProgress = 0;
    }

    private void tickTraveling(XoonglinEntity entity) {
        if (entity.position().distanceTo(Vec3.atCenterOf(workshopKeyBlock))
                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            entity.getNavigation().stop();
            state = State.CRAFTING;
            return;
        }
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, workshopKeyBlock);
        }
    }

    private void tickCrafting(ServerLevel level, XoonglinEntity entity, CrafterJob job) {
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(workshopKeyBlock))
                > CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            state = State.TRAVELING;
            repathTimer = 0;
            return;
        }

        List<Container> containers = findWorkshopContainers(level, job);
        if (containers.isEmpty()) {
            waitTicks = INGREDIENT_RETRY_COOLDOWN;
            return;
        }

        if (craftProgress == 0 && !ContainerUtil.hasAllIngredients(containers, job.getIngredients())) {
            LOGGER.warn("[Craft] {} is missing ingredients at workshop {}, waiting",
                    entity.getName().getString(), workshopKeyBlock);
            waitTicks = INGREDIENT_RETRY_COOLDOWN;
            return;
        }

        craftProgress++;
        if (craftProgress % 20 == 0) {
            entity.swing(InteractionHand.MAIN_HAND);
        }

        if (craftProgress >= job.getCraftingTime()) {
            craftProgress = 0;
            // Ingredients may have been taken away while crafting; verify before consuming
            if (!ContainerUtil.hasAllIngredients(containers, job.getIngredients())) {
                return;
            }
            ContainerUtil.consumeIngredients(containers, job.getIngredients());
            ItemStack leftover = ContainerUtil.insertIntoContainers(containers, job.createOutputStack());
            if (!leftover.isEmpty()) {
                Containers.dropItemStack(level, workshopKeyBlock.getX() + 0.5,
                        workshopKeyBlock.getY() + 1.0, workshopKeyBlock.getZ() + 0.5, leftover);
            }
        }
    }

    private List<Container> findWorkshopContainers(ServerLevel level, CrafterJob job) {
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        Structure workshop = data.getStructures().stream()
                .filter(s -> s.getKeyBlockPos().equals(workshopKeyBlock))
                .filter(s -> s.getStructureTypeId().equals(job.getRequiredStructureType()))
                .findFirst().orElse(null);
        if (workshop == null) return List.of();
        return ContainerUtil.findContainers(level, workshop);
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

}
