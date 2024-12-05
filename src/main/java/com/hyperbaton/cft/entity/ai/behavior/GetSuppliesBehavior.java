package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GetSuppliesBehavior extends Behavior<XunguiEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final double CLOSE_ENOUGH_DISTANCE_TO_CONTAINER = 1.25;

    public GetSuppliesBehavior(Map pEntryCondition) {
        super(pEntryCondition);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XunguiEntity mob) {
        LOGGER.trace("Starting behavior for getting supplies");
        // Only start if the Xungui has a home with a container
        return mob.getBrain().hasMemoryValue(homeContainerMemoryType()) &&
                !mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLY_COOLDOWN.get());
    }

    @Override
    protected void start(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {
        mob.getNavigation().moveTo(
                mob.getNavigation().createPath(mob.getBrain().getMemory(homeContainerMemoryType()).get(),
                        1),
                1);
    }

    @Override
    protected void tick(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {
        if (isCloseEnoughToContainer(mob)) {
            mob.getNavigation().stop(); // Stop moving once close
        }
    }

    @Override
    protected void stop(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {

        LOGGER.trace("Checking container in home");
        if (!mob.getBrain().hasMemoryValue(homeContainerMemoryType()) ||
                !isCloseEnoughToContainer(mob)) {
            return;
        }
        Container container = (Container) mob.level().getBlockEntity(mob.getBrain().getMemory(homeContainerMemoryType()).get());
        // If there is no chest, the Xungui doesn't have a home and should not try to get anything
        if (container == null) {
            mob.getBrain().eraseMemory(homeContainerMemoryType());
            return;
        }

        mob.getBrain().getMemory(suppliesNeededMemoryType()).ifPresent(
                suppliesNeeded -> suppliesNeeded.stream()
                        // Check if inventory is full
                        .filter(itemToRetrieve -> mob.getInventory().canAddItem(itemToRetrieve))
                        .forEach(itemToRetrieve -> findItemPositionInContainer(container, itemToRetrieve)
                                .ifPresent(position -> mob.getInventory()
                                        .addItem(container.removeItem(position, itemToRetrieve.getCount()))))
        );
        mob.getBrain().eraseMemory(suppliesNeededMemoryType());
        // Add a cooldown for retrying
        mob.getBrain().setMemoryWithExpiry(CftMemoryModuleType.SUPPLY_COOLDOWN.get(), true, 200L);
    }

    @Override
    protected boolean canStillUse(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {
        return mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLIES_NEEDED.get())
                && mob.getBrain().hasMemoryValue(homeContainerMemoryType())
                && !isCloseEnoughToContainer(mob)
                && !mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLY_COOLDOWN.get());
    }

    private boolean isCloseEnoughToContainer(XunguiEntity mob) {
        return mob.getBrain()
                .getMemory(homeContainerMemoryType()).map(
                        containerPos -> mob.position().distanceTo(containerPos.getCenter()) < CLOSE_ENOUGH_DISTANCE_TO_CONTAINER
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

    private MemoryModuleType<List<ItemStack>> suppliesNeededMemoryType() {
        return CftMemoryModuleType.SUPPLIES_NEEDED.get();
    }

    private MemoryModuleType<BlockPos> homeContainerMemoryType() {
        return CftMemoryModuleType.HOME_CONTAINER_POSITION.get();
    }
}
