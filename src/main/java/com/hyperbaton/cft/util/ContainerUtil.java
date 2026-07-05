package com.hyperbaton.cft.util;

import com.hyperbaton.cft.job.ItemQuantity;
import com.hyperbaton.cft.structure.Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helpers for jobs that read from and write to the containers of a structure. A
 * structure may hold several containers — including machine blocks like furnaces and
 * smokers, which also implement Container — so items must always be looked for across
 * all of them, and plain storage (chests, barrels...) is preferred over machine slots.
 */
public class ContainerUtil {

    /**
     * All container positions of a structure, plain storage first, machine blocks
     * (WorldlyContainer: furnaces, smokers, brewing stands...) last.
     */
    public static List<BlockPos> findContainerPositions(ServerLevel level, Structure structure) {
        Set<BlockPos> plain = new LinkedHashSet<>();
        Set<BlockPos> machines = new LinkedHashSet<>();
        for (List<BlockPos> blocks : structure.getBlockPositions().values()) {
            for (BlockPos pos : blocks) {
                if (level.getBlockEntity(pos) instanceof Container container) {
                    if (container instanceof WorldlyContainer) {
                        machines.add(pos);
                    } else {
                        plain.add(pos);
                    }
                }
            }
        }
        List<BlockPos> result = new ArrayList<>(plain);
        result.addAll(machines);
        return result;
    }

    /**
     * All containers of a structure, in the same order as findContainerPositions.
     */
    public static List<Container> findContainers(ServerLevel level, Structure structure) {
        List<Container> result = new ArrayList<>();
        for (BlockPos pos : findContainerPositions(level, structure)) {
            if (level.getBlockEntity(pos) instanceof Container container) {
                result.add(container);
            }
        }
        return result;
    }

    /**
     * Inserts a stack into a container, merging with existing stacks first.
     *
     * @return the remainder that did not fit
     */
    public static ItemStack insertIntoContainer(Container container, ItemStack stack) {
        ItemStack toInsert = stack.copy();
        for (int i = 0; i < container.getContainerSize() && !toInsert.isEmpty(); i++) {
            ItemStack existing = container.getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, toInsert)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int transfer = Math.min(space, toInsert.getCount());
                existing.grow(transfer);
                toInsert.shrink(transfer);
            }
        }
        for (int i = 0; i < container.getContainerSize() && !toInsert.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, toInsert.copy());
                toInsert = ItemStack.EMPTY;
            }
        }
        return toInsert;
    }

    /**
     * Inserts a stack across several containers, in order.
     *
     * @return the remainder that did not fit anywhere
     */
    public static ItemStack insertIntoContainers(List<Container> containers, ItemStack stack) {
        ItemStack remainder = stack;
        for (Container container : containers) {
            remainder = insertIntoContainer(container, remainder);
            container.setChanged();
            if (remainder.isEmpty()) break;
        }
        return remainder;
    }

    /**
     * True if the containers together hold every ingredient in the required quantity.
     */
    public static boolean hasAllIngredients(List<Container> containers, List<ItemQuantity> ingredients) {
        for (ItemQuantity ingredient : ingredients) {
            int found = 0;
            for (Container container : containers) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                        found += stack.getCount();
                    }
                }
            }
            if (found < ingredient.quantity()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes the given ingredients from the containers, in order.
     */
    public static void consumeIngredients(List<Container> containers, List<ItemQuantity> ingredients) {
        for (ItemQuantity ingredient : ingredients) {
            int remaining = ingredient.quantity();
            for (Container container : containers) {
                for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                        int take = Math.min(remaining, stack.getCount());
                        container.removeItem(i, take);
                        remaining -= take;
                    }
                }
                container.setChanged();
                if (remaining <= 0) break;
            }
        }
    }
}
