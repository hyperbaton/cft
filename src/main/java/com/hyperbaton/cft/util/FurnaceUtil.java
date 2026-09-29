package com.hyperbaton.cft.util;

import com.hyperbaton.cft.structure.Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Tends vanilla furnaces, smokers and blast furnaces with the goods kept in the plain
 * containers (chests, barrels...) of the same structure: collects their results and loads
 * them with things to cook and fuel. The furnaces themselves do the cooking, so recipes, fuel
 * values and cooking times are all vanilla's.
 */
public final class FurnaceUtil {
    private FurnaceUtil() {}

    // Same slots as AbstractFurnaceBlockEntity, which keeps its constants protected
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_FUEL = 1;
    private static final int SLOT_RESULT = 2;
    /** How much fuel a furnace is kept stocked with while it has something to cook. */
    private static final int FUEL_TARGET = 8;

    /** Vanilla furnaces, smokers and blast furnaces of the structure. */
    public static List<AbstractFurnaceBlockEntity> findFurnaces(ServerLevel level, Structure structure) {
        List<AbstractFurnaceBlockEntity> furnaces = new ArrayList<>();
        for (BlockPos pos : structure.getAllBlockPositions()) {
            if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace && recipeTypeOf(furnace).isPresent()) {
                furnaces.add(furnace);
            }
        }
        return furnaces;
    }

    /** Plain storage of the structure, leaving out furnaces and other machines. */
    public static List<Container> findStorage(ServerLevel level, Structure structure) {
        List<Container> storage = new ArrayList<>();
        for (BlockPos pos : structure.getAllBlockPositions()) {
            if (level.getBlockEntity(pos) instanceof Container container && !(container instanceof WorldlyContainer)) {
                storage.add(container);
            }
        }
        return storage;
    }

    /**
     * Does whatever the furnace needs: takes out its result, clears leftovers from its fuel slot
     * (like the empty bucket of a lava bucket), and loads it with something to cook that
     * {@code accepts} allows, plus fuel.
     *
     * @return whether anything was moved
     */
    public static boolean tend(Level level, AbstractFurnaceBlockEntity furnace, List<Container> storage,
                               Predicate<ItemStack> accepts) {
        Optional<RecipeType<? extends AbstractCookingRecipe>> recipeType = recipeTypeOf(furnace);
        if (recipeType.isEmpty()) return false;

        boolean changed = moveToStorage(furnace, SLOT_RESULT, storage);
        ItemStack fuel = furnace.getItem(SLOT_FUEL);
        if (!fuel.isEmpty() && !furnace.canPlaceItem(SLOT_FUEL, fuel)) {
            changed |= moveToStorage(furnace, SLOT_FUEL, storage);
        }
        changed |= loadInput(level, furnace, recipeType.get(), storage, accepts);
        if (!furnace.getItem(SLOT_INPUT).isEmpty()) {
            changed |= loadFuel(furnace, storage);
        }
        if (changed) {
            furnace.setChanged();
        }
        return changed;
    }

    /**
     * The recipes a furnace cooks. Only the vanilla ones are known, since vanilla keeps the
     * recipe type private; furnaces from other mods are left alone.
     */
    private static Optional<RecipeType<? extends AbstractCookingRecipe>> recipeTypeOf(AbstractFurnaceBlockEntity furnace) {
        if (furnace instanceof BlastFurnaceBlockEntity) return Optional.of(RecipeType.BLASTING);
        if (furnace instanceof SmokerBlockEntity) return Optional.of(RecipeType.SMOKING);
        if (furnace instanceof FurnaceBlockEntity) return Optional.of(RecipeType.SMELTING);
        return Optional.empty();
    }

    private static boolean loadInput(Level level, AbstractFurnaceBlockEntity furnace,
                                     RecipeType<? extends AbstractCookingRecipe> recipeType,
                                     List<Container> storage, Predicate<ItemStack> accepts) {
        ItemStack input = furnace.getItem(SLOT_INPUT);
        if (input.isEmpty()) {
            ItemStack taken = takeFromStorage(storage,
                    stack -> accepts.test(stack) && canCook(level, recipeType, stack), Integer.MAX_VALUE);
            furnace.setItem(SLOT_INPUT, taken);
            return !taken.isEmpty();
        }
        int space = input.getMaxStackSize() - input.getCount();
        if (space <= 0) return false;
        ItemStack taken = takeFromStorage(storage, stack -> ItemStack.isSameItemSameComponents(stack, input), space);
        input.grow(taken.getCount());
        return !taken.isEmpty();
    }

    private static boolean loadFuel(AbstractFurnaceBlockEntity furnace, List<Container> storage) {
        ItemStack fuel = furnace.getItem(SLOT_FUEL);
        if (fuel.isEmpty()) {
            // An empty bucket can go in the fuel slot, but doesn't burn
            ItemStack taken = takeFromStorage(storage,
                    stack -> !stack.is(Items.BUCKET) && furnace.canPlaceItem(SLOT_FUEL, stack), FUEL_TARGET);
            furnace.setItem(SLOT_FUEL, taken);
            return !taken.isEmpty();
        }
        int missing = Math.min(FUEL_TARGET, fuel.getMaxStackSize()) - fuel.getCount();
        if (missing <= 0) return false;
        ItemStack taken = takeFromStorage(storage, stack -> ItemStack.isSameItemSameComponents(stack, fuel), missing);
        fuel.grow(taken.getCount());
        return !taken.isEmpty();
    }

    private static <T extends AbstractCookingRecipe> boolean canCook(Level level, RecipeType<T> recipeType, ItemStack stack) {
        return level.getRecipeManager().getRecipeFor(recipeType, new SingleRecipeInput(stack), level).isPresent();
    }

    /** Moves a furnace slot into the storage; whatever doesn't fit stays in the furnace. */
    private static boolean moveToStorage(AbstractFurnaceBlockEntity furnace, int slot, List<Container> storage) {
        ItemStack stack = furnace.getItem(slot);
        if (stack.isEmpty()) return false;
        ItemStack remainder = ContainerUtil.insertIntoContainers(storage, stack);
        furnace.setItem(slot, remainder);
        return remainder.getCount() != stack.getCount();
    }

    /** Takes up to {@code maxCount} items of the first stack in the storage that matches. */
    private static ItemStack takeFromStorage(List<Container> storage, Predicate<ItemStack> matches, int maxCount) {
        for (Container container : storage) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && matches.test(stack)) {
                    ItemStack taken = container.removeItem(i, Math.min(maxCount, stack.getCount()));
                    container.setChanged();
                    return taken;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
