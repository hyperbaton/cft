package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GetSuppliesBehavior extends Behavior<XoonglinEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public GetSuppliesBehavior(Map pEntryCondition) {
        super(pEntryCondition);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity mob) {
        LOGGER.trace("Starting behavior for getting supplies");
        // Only start if the Xoonglin has a home with a container
        return mob.getBrain().hasMemoryValue(homeContainerMemoryType()) &&
                !mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLY_COOLDOWN.get());
    }

    @Override
    protected void start(ServerLevel pLevel, XoonglinEntity mob, long pGameTime) {
        mob.getNavigation().moveTo(
                mob.getNavigation().createPath(mob.getBrain().getMemory(homeContainerMemoryType()).get(),
                        1),
                1);
    }

    @Override
    protected void tick(ServerLevel pLevel, XoonglinEntity mob, long pGameTime) {
        if (isCloseEnoughToContainer(mob)) {
            mob.getNavigation().stop(); // Stop moving once close
        }
    }

    @Override
    protected void stop(ServerLevel pLevel, XoonglinEntity mob, long pGameTime) {
        LOGGER.debug("Checking container in home");
        if (!mob.getBrain().hasMemoryValue(homeContainerMemoryType()) ||
                !isCloseEnoughToContainer(mob)) {
            return;
        }

        Optional<Container> container = mob.getBrain().getMemory(homeContainerMemoryType())
                .map(blockPos -> (Container) mob.level().getBlockEntity(blockPos));

        if (container.isEmpty()) {
            // If the home container is missing, forget about it
            mob.getBrain().eraseMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get());
            return;
        }

        Optional<List<Ingredient>> neededSupplies = mob.getBrain().getMemory(CftMemoryModuleType.SUPPLIES_NEEDED.get());
        if (neededSupplies.isEmpty()) {
            return;
        }

        List<Ingredient> ingredientsNeeded = neededSupplies.get();
        retrieveSupplies(mob, container.get(), ingredientsNeeded);

        // Once supplies are retrieved, remove the memory
        mob.getBrain().eraseMemory(CftMemoryModuleType.SUPPLIES_NEEDED.get());
        // Add a cooldown before requesting new supplies
        mob.getBrain().setMemoryWithExpiry(CftMemoryModuleType.SUPPLY_COOLDOWN.get(), true, CftConfig.SUPPLY_COOLDOWN.get());
    }

    /**
     * Tries to retrieve the needed supplies from the container.
     */
    private void retrieveSupplies(XoonglinEntity mob, Container container, List<Ingredient> neededSupplies) {
        for (Ingredient ingredient : neededSupplies) {
            if (!canStoreItem(mob, ingredient)) {
                LOGGER.warn("Xoonglin's inventory is full, cannot retrieve supplies.");
                return;
            }

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);

                if (ingredient.test(stack) && !stack.isEmpty()) {
                    int neededAmount = getNeededQuantity(mob, ingredient);
                    int takenAmount = Math.min(neededAmount, stack.getCount());

                    ItemStack takenStack = container.removeItem(i, takenAmount);
                    mob.getInventory().addItem(takenStack);

                    // Stop looking for this supply if we got enough
                    break;
                }
            }
        }
    }

    /**
     * Checks if the Xoonglin has space for the given ingredient.
     */
    private boolean canStoreItem(XoonglinEntity mob, Ingredient ingredient) {
        return mob.getInventory().canAddItem(ingredient.getItems()[0]); // Check using first matching item
    }

    /**
     * Determines how much of an ingredient the Xoonglin needs.
     * (This method could be extended if we want different amounts per ingredient.)
     */
    private int getNeededQuantity(XoonglinEntity mob, Ingredient ingredient) {
        return 1; // Defaulting to 1 for now; can be adjusted dynamically.
    }

    @Override
    protected boolean canStillUse(ServerLevel pLevel, XoonglinEntity mob, long pGameTime) {
        return mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLIES_NEEDED.get())
                && mob.getBrain().hasMemoryValue(homeContainerMemoryType())
                && !isCloseEnoughToContainer(mob)
                && !mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLY_COOLDOWN.get());
    }

    private boolean isCloseEnoughToContainer(XoonglinEntity mob) {
        return mob.getBrain()
                .getMemory(homeContainerMemoryType()).map(
                        containerPos -> mob.position().distanceTo(containerPos.getCenter())
                                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()
                ).orElse(false);
    }

    private Optional<Integer> findItemPositionInContainer(Container container, ItemStack itemToRetrieve) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(itemToRetrieve.getItem()) && stack.getCount() >= itemToRetrieve.getCount()) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private MemoryModuleType<List<Ingredient>> suppliesNeededMemoryType() {
        return CftMemoryModuleType.SUPPLIES_NEEDED.get();
    }

    private MemoryModuleType<BlockPos> homeContainerMemoryType() {
        return CftMemoryModuleType.HOME_CONTAINER_POSITION.get();
    }
}
