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

    public static boolean isAtHome(XoonglinEntity x, double radius) {
        if (x.getHome() == null) return false;
        BlockPos homePos = x.getHome().getEntrance();
        return homePos != null && homePos.closerToCenterThan(x.position(), radius);
    }

    public static ItemStack tryDepositAtHome(XoonglinEntity x, ItemStack stack) {
        Optional<IItemHandler> inv = findHomeInventory(x);
        if (inv.isPresent()) {
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(inv.get(), stack, false);
            return remaining;
        }
        return stack;
    }

    public static void dropAtHome(XoonglinEntity x, ItemStack stack) {
        if (stack.isEmpty()) return;
        Level level = x.level();
        BlockPos pos = x.getHome() != null && x.getHome().getEntrance() != null
                ? x.getHome().getEntrance()
                : x.blockPosition();
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
    }

    private static Optional<IItemHandler> findHomeInventory(XoonglinEntity x) {
        Level level = x.level();
        if (x.getHome() != null) {
            BlockPos chestPos = x.getHome().getContainerPos(); // adjust to your API
            if (chestPos != null) {
                BlockEntity be = level.getBlockEntity(chestPos);
                if (be != null) {
                    return be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                }
            }
            // Fallback: look for any inventory near home center
            BlockPos center = x.getHome().getEntrance();
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