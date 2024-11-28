package com.hyperbaton.cft.entity.behavior;

import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.entity.memory.CftMemoryModuleType;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Map;

public class GetSuppliesBehavior extends Behavior<XunguiEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public GetSuppliesBehavior(Map pEntryCondition) {
        super(pEntryCondition);
    }

    // TODO: There should be some delay after the mob doesn't find any supplies
    @Override
    protected void start(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {
        startWalkingTowards(mob, mob.getBrain().getMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get()).get());
    }

    @Override
    protected void tick(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {
        if (mob.position().distanceTo(mob.getBrain()
                .getMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get()).get().getCenter()) <= 1.0) {
            mob.getNavigation().stop(); // Stop moving once close
        }
    }

    @Override
    protected void stop(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {

        LOGGER.debug("Checking container in home");
        if(!mob.getBrain().hasMemoryValue(CftMemoryModuleType.HOME_CONTAINER_POSITION.get())) {
            return;
        }
        Container container = (Container) mob.level().getBlockEntity(mob.getBrain().getMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get()).get());
        // If there is no chest, the Xungui doesn't have a home and should not try to get anything
        if (container == null) {
            mob.getBrain().eraseMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get());
            return;
        }

        ItemStack itemsToRetrieve = mob.getBrain().getMemory(CftMemoryModuleType.SUPPLIES_NEEDED.get()).get();
        if (!mob.getInventory().canAddItem(itemsToRetrieve)) {
            LOGGER.warn("Xungui's inventory is full, cannot retrieve supplies.");
            return;
        }
        if (container.hasAnyMatching(stack -> stack.is(itemsToRetrieve.getItem())
                && stack.getCount() >= itemsToRetrieve.getCount())) {
            int latestStickSlot = -1;
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).is(itemsToRetrieve.getItem())
                        && container.getItem(i).getCount() >= itemsToRetrieve.getCount()) {
                    latestStickSlot = i;
                    break;
                }
            }
            if (latestStickSlot >= 0) {
                mob.getInventory().addItem(container.removeItem(latestStickSlot, itemsToRetrieve.getCount()));
            }
        }
        mob.getBrain().eraseMemory(CftMemoryModuleType.SUPPLIES_NEEDED.get());
        // Add a cooldown for retrying
        mob.getBrain().setMemoryWithExpiry(CftMemoryModuleType.SUPPLY_COOLDOWN.get(), true, 200L);
    }

    @Override
    protected boolean canStillUse(ServerLevel pLevel, XunguiEntity mob, long pGameTime) {
        return mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLIES_NEEDED.get())
                && mob.getBrain().hasMemoryValue(CftMemoryModuleType.HOME_CONTAINER_POSITION.get())
                && mob.position().distanceTo(mob.getBrain()
                .getMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get())
                .get().getCenter()) > 1.0
                && !mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLY_COOLDOWN.get());
    }

    private void startWalkingTowards(XunguiEntity mob, BlockPos pos) {
        BehaviorUtils.setWalkAndLookTargetMemories(mob, pos, 1f, 1);
    }
}