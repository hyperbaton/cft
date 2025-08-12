package com.hyperbaton.cft.util;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Optional;

public final class JobUtil {
    private JobUtil() {}

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
            BlockPos chestPos = mob.getHome().getContainerPos();
            if (chestPos != null) {
                BlockEntity be = level.getBlockEntity(chestPos);
                if (be != null) {
                    return be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                }
            }
            // Fallback: look for any inventory near home center
            BlockPos center = mob.getHome().getEntrance();
            if (center != null) {
                int r = 5;
                for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
                    BlockEntity be = level.getBlockEntity(p);
                    if (be == null) continue;
                    Optional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                    if (cap.isPresent()) return cap;
                }
            }
        }
        return Optional.empty();
    }
}