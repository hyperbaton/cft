package com.hyperbaton.cft.util;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.network.InventorySlotData;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.network.JobStatus;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.ai.ErrandUtils;
import com.hyperbaton.cft.entity.ai.behavior.WorkStep;
import com.hyperbaton.cft.entity.ai.schedule.ScheduleUtils;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureUtils;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JobUtil {
    private JobUtil() {}

    public static final int TICKS_PER_MC_HOUR = 1000;

    public static String formatWorkTime(int workedTicks, double hoursPerDay) {
        int workedHours = workedTicks / TICKS_PER_MC_HOUR;
        int workedMinutes = (workedTicks % TICKS_PER_MC_HOUR) * 60 / TICKS_PER_MC_HOUR;
        int neededHours = (int) Math.round(hoursPerDay);
        return String.format("%d:%02d / %d", workedHours, workedMinutes, neededHours);
    }

    public static JobInfoData buildJobInfo(XoonglinEntity xoonglin) {
        if (xoonglin.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(xoonglin.getJob());
        if (job == null) return null;
        JobInfoData info = job.getDisplayInfo(xoonglin, xoonglin.getJobState());
        if (info == null) return null;
        // Outside working time, the job's own status would describe work that isn't happening
        if (xoonglin.isSleeping()) {
            return new JobInfoData(JobStatus.SLEEPING, info.entries());
        }
        if (ScheduleUtils.isOffDuty(xoonglin)) {
            JobStatus status = xoonglin.getBrain().isActive(Activity.REST) ? JobStatus.AT_HOME : freeTimeStatus(xoonglin);
            return new JobInfoData(status, info.entries());
        }
        // Errands pause the job, so its status would describe work it isn't doing now
        if (ErrandUtils.hasErrands(xoonglin)) {
            return new JobInfoData(JobStatus.PAUSED.withDetail(errandsText(xoonglin)), info.entries());
        }
        return info;
    }

    /** The errands pausing its job, as named by their lang entries {@code errand.<namespace>.<path>}. */
    private static Component errandsText(XoonglinEntity xoonglin) {
        MutableComponent text = Component.empty();
        for (ResourceLocation errand : ErrandUtils.errands(xoonglin)) {
            if (!text.getSiblings().isEmpty()) {
                text.append(", ");
            }
            text.append(Component.translatable("errand." + errand.getNamespace() + "." + errand.getPath()));
        }
        return text;
    }

    /** {@link JobStatus#WORKING}, with the step of its work it's on if its behavior shows one. */
    public static JobStatus workingStatus(XoonglinEntity xoonglin) {
        return JobStatus.WORKING.withDetail(xoonglin.getBrain().getMemory(CftMemoryModuleType.WORK_STEP.get())
                .map(step -> (Component) Component.translatable(step.key()))
                .orElse(null));
    }

    private static JobStatus freeTimeStatus(XoonglinEntity xoonglin) {
        Brain<XoonglinEntity> brain = xoonglin.getBrain();
        if (brain.hasMemoryValue(CftMemoryModuleType.CONVERSATION_PARTNER.get())) {
            return JobStatus.CHATTING;
        }
        if (brain.hasMemoryValue(CftMemoryModuleType.VISITING.get()) || brain.hasMemoryValue(CftMemoryModuleType.MUST_VISIT.get())) {
            return JobStatus.VISITING;
        }
        return JobStatus.FREE_TIME;
    }

    public static List<InventorySlotData> buildInventoryData(XoonglinEntity xoonglin) {
        List<InventorySlotData> slots = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = xoonglin.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                slots.add(new InventorySlotData(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount()));
            }
        }
        for (int i = 0; i < xoonglin.getInventory().getContainerSize(); i++) {
            ItemStack stack = xoonglin.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                slots.add(new InventorySlotData(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount()));
            }
        }
        return slots;
    }

    /**
     * The tool of this kind (e.g. {@code ItemTags.AXES}) the Xoonglin holds in its main hand or
     * carries in its inventory, or EMPTY if it has none.
     */
    public static ItemStack findTool(XoonglinEntity xoonglin, TagKey<Item> tool) {
        if (xoonglin.getMainHandItem().is(tool)) return xoonglin.getMainHandItem();
        for (int i = 0; i < xoonglin.getInventory().getContainerSize(); i++) {
            ItemStack stack = xoonglin.getInventory().getItem(i);
            if (stack.is(tool)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Holds a tool of this kind from its inventory in its main hand, to show it. Only if that hand
     * is free: an equipment need for it takes precedence, and the tool then stays in the inventory.
     */
    public static void equipTool(XoonglinEntity xoonglin, TagKey<Item> tool) {
        if (!xoonglin.getMainHandItem().isEmpty()) return;
        for (int i = 0; i < xoonglin.getInventory().getContainerSize(); i++) {
            if (xoonglin.getInventory().getItem(i).is(tool)) {
                xoonglin.setItemSlot(EquipmentSlot.MAINHAND, xoonglin.getInventory().removeItem(i, 1));
                return;
            }
        }
    }

    /**
     * Whether the structure is running: one of its workers is working right now. A worker is a
     * Xoonglin assigned to it through a job that requires its structure type. With work steps (e.g.
     * "tending_furnaces"), the worker must also be on one of them, as shown in its job tab.
     */
    public static boolean isStructureRunning(ServerLevel level, Structure structure, List<String> workSteps) {
        for (UUID userId : structure.getUserIds()) {
            if (level.getEntity(userId) instanceof XoonglinEntity worker && isWorkingFor(worker, structure)
                    && (workSteps.isEmpty() || isOnWorkStep(worker, workSteps))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The structure of a type the Xoonglin is assigned to, checked to be still standing right before
     * it's used. If it's gone (torn down or unregistered), the Xoonglin gives it up, so its job or need
     * looks for another one. A structure its job takes apart, like a quarry, isn't detected again.
     */
    public static Optional<Structure> checkAssignedStructure(XoonglinEntity xoonglin, String structureTypeId) {
        BlockPos keyBlockPos = xoonglin.getAssignedStructurePos(structureTypeId);
        if (keyBlockPos == null) return Optional.empty();
        ServerLevel level = (ServerLevel) xoonglin.level();
        Optional<Structure> structure = consumesStructure(xoonglin, structureTypeId)
                ? StructuresData.get(level).findByKeyBlock(keyBlockPos)
                        .filter(registered -> registered.getStructureTypeId().equals(structureTypeId))
                : StructureUtils.recheck(level, keyBlockPos, structureTypeId);
        if (structure.isEmpty()) {
            xoonglin.unassignStructure(structureTypeId);
        }
        return structure;
    }

    /**
     * For a job about to set its work memory: whether its workplace (if it has one) is still standing.
     * It's only checked when the Xoonglin starts working (the memory isn't set yet), not on every
     * tick of work; if it's gone, the Xoonglin gives it up and the job looks for another one.
     */
    public static boolean checkWorkplace(XoonglinEntity xoonglin, Job job) {
        String structureTypeId = job.getRequiredStructureType();
        if (structureTypeId == null || xoonglin.getAssignedStructurePos(structureTypeId) == null
                || xoonglin.getBrain().hasMemoryValue(job.getWorkMemory())) {
            return true;
        }
        return checkAssignedStructure(xoonglin, structureTypeId).isPresent();
    }

    /** Whether the structure type is its job's workplace and the job takes it apart, like a quarry. */
    private static boolean consumesStructure(XoonglinEntity xoonglin, String structureTypeId) {
        Job job = xoonglin.getJob() != null ? CftRegistry.JOBS.get(xoonglin.getJob()) : null;
        return job != null && job.consumesStructure() && structureTypeId.equals(job.getRequiredStructureType());
    }

    private static boolean isWorkingFor(XoonglinEntity worker, Structure structure) {
        Job job = worker.getJob() != null ? CftRegistry.JOBS.get(worker.getJob()) : null;
        String structureType = structure.getStructureTypeId();
        return job != null && structureType.equals(job.getRequiredStructureType())
                && structure.getKeyBlockPos().equals(worker.getAssignedStructurePos(structureType))
                && worker.isWorkingAtJob();
    }

    private static boolean isOnWorkStep(XoonglinEntity worker, List<String> workSteps) {
        return worker.getBrain().getMemory(CftMemoryModuleType.WORK_STEP.get())
                .map(step -> workSteps.stream().anyMatch(name -> WorkStep.of(name).equals(step)))
                .orElse(false);
    }

    public static boolean isAtHome(XoonglinEntity mob, double radius) {
        if (mob.getHome() == null) return false;
        BlockPos homePos = mob.getHome().getEntrance();
        return homePos != null && homePos.closerToCenterThan(mob.position(), radius);
    }

    public static ItemStack tryDepositAtHome(XoonglinEntity mob, ItemStack stack) {
        return findHomeInventory(mob)
                .map(iItemHandler -> ItemHandlerHelper
                        .insertItemStacked(iItemHandler, stack, false))
                .orElse(stack);
    }

    public static void dropAtHome(XoonglinEntity mob, ItemStack stack) {
        if (stack.isEmpty()) return;
        Level level = mob.level();
        BlockPos pos = mob.getHome() != null && mob.getHome().getEntrance() != null
                ? mob.getHome().getEntrance()
                : mob.blockPosition();
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
    }

    private static Optional<IItemHandler> findHomeInventory(XoonglinEntity mob) {
        Level level = mob.level();
        if (mob.getHome() != null) {
            for (BlockPos pos : mob.getHome().getInteriorBlocks()) {
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                if (handler != null) {
                    return Optional.of(handler);
                }
            }
        }
        return Optional.empty();
    }
}
