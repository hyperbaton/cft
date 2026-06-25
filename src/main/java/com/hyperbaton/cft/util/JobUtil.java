package com.hyperbaton.cft.util;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.network.JobInfoData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Optional;

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
        return job.getDisplayInfo(xoonglin, xoonglin.getJobState());
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
