package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftDataComponents;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.item.ManuscriptData;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.job.ScribeJob;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Makes a scribe work at its scriptorium: stands by the key block, takes the needed
 * input into its own inventory first, then works for crafting_time before consuming the
 * input from its inventory and depositing a new copy. The source book is never
 * consumed. If nothing eligible is found, it waits and rechecks periodically.
 */
public class ScribeBehavior extends Behavior<XoonglinEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REPATH_INTERVAL = 40;
    // Ticks to wait before re-checking the container when there's nothing to copy
    private static final int NO_SOURCE_RETRY_COOLDOWN = 600;

    private enum State {
        TRAVELING, FETCHING, SCRIBING
    }

    private State state;
    private BlockPos scriptoriumKeyBlock;
    private int repathTimer;
    private int waitTicks;
    private int scribeProgress;

    public ScribeBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        ScribeJob job = getScribeJob(entity);
        return job != null && entity.getAssignedStructurePos(job.getRequiredStructureType()) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        ScribeJob job = getScribeJob(entity);
        scriptoriumKeyBlock = entity.getAssignedStructurePos(job.getRequiredStructureType());
        state = State.TRAVELING;
        repathTimer = 0;
        waitTicks = 0;
        scribeProgress = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_SCRIBE.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        ScribeJob job = getScribeJob(entity);
        if (job == null || scriptoriumKeyBlock == null) return;

        switch (state) {
            case TRAVELING -> tickTraveling(entity, job);
            case FETCHING -> tickFetching(level, entity, job);
            case SCRIBING -> tickScribing(level, entity, job);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
        scribeProgress = 0;
    }

    private void tickTraveling(XoonglinEntity entity, ScribeJob job) {
        if (entity.position().distanceTo(Vec3.atCenterOf(scriptoriumKeyBlock))
                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            entity.getNavigation().stop();
            state = hasInput(entity, job) ? State.SCRIBING : State.FETCHING;
            scribeProgress = 0;
            return;
        }
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, scriptoriumKeyBlock);
        }
    }

    private void tickFetching(ServerLevel level, XoonglinEntity entity, ScribeJob job) {
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        List<Container> containers = findScriptoriumContainers(level, job);
        if (containers.isEmpty() || !ContainerUtil.hasAllIngredients(containers, List.of(job.getInput()))) {
            waitTicks = NO_SOURCE_RETRY_COOLDOWN;
            return;
        }

        takeInput(entity, containers, job);
        containers.forEach(Container::setChanged);
        state = State.SCRIBING;
        scribeProgress = 0;
    }

    private void tickScribing(ServerLevel level, XoonglinEntity entity, ScribeJob job) {
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(scriptoriumKeyBlock))
                > CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            state = State.TRAVELING;
            repathTimer = 0;
            return;
        }

        if (!hasInput(entity, job)) {
            state = State.FETCHING;
            return;
        }

        List<Container> containers = findScriptoriumContainers(level, job);
        if (containers.isEmpty()) {
            waitTicks = NO_SOURCE_RETRY_COOLDOWN;
            return;
        }

        if (scribeProgress == 0 && findSource(containers, entity.getLeaderId()) == null) {
            waitTicks = NO_SOURCE_RETRY_COOLDOWN;
            return;
        }

        scribeProgress++;
        if (scribeProgress % 20 == 0) {
            entity.swing(InteractionHand.MAIN_HAND);
        }

        if (scribeProgress >= job.getCraftingTime()) {
            scribeProgress = 0;
            ItemStack source = findSource(containers, entity.getLeaderId());
            if (source == null) {
                return;
            }

            WrittenBookContent sourceContent = source.get(DataComponents.WRITTEN_BOOK_CONTENT);
            ManuscriptData manuscript = source.get(CftDataComponents.MANUSCRIPT.get());
            WrittenBookContent copyContent = sourceContent.tryCraftCopy();
            if (copyContent == null) return;

            consumeInput(entity, job);

            ItemStack copy = new ItemStack(source.getItem());
            copy.set(DataComponents.WRITTEN_BOOK_CONTENT, copyContent);
            copy.set(CftDataComponents.MANUSCRIPT.get(), manuscript);

            ItemStack leftover = ContainerUtil.insertIntoContainers(containers, copy);
            if (!leftover.isEmpty()) {
                Containers.dropItemStack(level, scriptoriumKeyBlock.getX() + 0.5,
                        scriptoriumKeyBlock.getY() + 1.0, scriptoriumKeyBlock.getZ() + 0.5, leftover);
            }
        }
    }

    private boolean hasInput(XoonglinEntity entity, ScribeJob job) {
        SimpleContainer inventory = entity.getInventory();
        int found = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && job.getInput().ingredient().test(stack)) {
                found += stack.getCount();
                if (found >= job.getInput().quantity()) return true;
            }
        }
        return false;
    }

    private void takeInput(XoonglinEntity entity, List<Container> containers, ScribeJob job) {
        int wanted = job.getInput().quantity();
        for (Container container : containers) {
            for (int i = 0; i < container.getContainerSize() && wanted > 0; i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty() || !job.getInput().ingredient().test(stack)) continue;
                int take = Math.min(wanted, stack.getCount());
                ItemStack taken = stack.copyWithCount(take);
                ItemStack leftover = entity.getInventory().addItem(taken);
                int actuallyTaken = take - leftover.getCount();
                stack.shrink(actuallyTaken);
                wanted -= actuallyTaken;
                if (!leftover.isEmpty()) {
                    wanted = 0; // inventory full
                }
            }
        }
    }

    private void consumeInput(XoonglinEntity entity, ScribeJob job) {
        SimpleContainer inventory = entity.getInventory();
        int remaining = job.getInput().quantity();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && job.getInput().ingredient().test(stack)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    /** First book in the containers with a manuscript component for this leader that can still be copied. */
    private ItemStack findSource(List<Container> containers, UUID leaderId) {
        if (leaderId == null) return null;
        for (Container container : containers) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
                ManuscriptData manuscript = stack.get(CftDataComponents.MANUSCRIPT.get());
                if (content == null || manuscript == null) continue;
                if (!manuscript.leaderId().equals(leaderId)) continue;
                if (content.tryCraftCopy() == null) continue;
                return stack;
            }
        }
        return null;
    }

    private List<Container> findScriptoriumContainers(ServerLevel level, ScribeJob job) {
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        Structure scriptorium = data.getStructures().stream()
                .filter(s -> s.getKeyBlockPos().equals(scriptoriumKeyBlock))
                .filter(s -> s.getStructureTypeId().equals(job.getRequiredStructureType()))
                .findFirst().orElse(null);
        if (scriptorium == null) return List.of();
        return ContainerUtil.findContainers(level, scriptorium);
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private ScribeJob getScribeJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof ScribeJob s ? s : null;
    }
}
