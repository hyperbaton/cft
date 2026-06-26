package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.need.GoodsNeed;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GetSuppliesBehavior extends Behavior<XoonglinEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public GetSuppliesBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity mob) {
        LOGGER.trace("Starting behavior for getting supplies");
        return mob.getBrain().hasMemoryValue(CftMemoryModuleType.HOME_CONTAINER.get()) &&
                !mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLY_COOLDOWN.get());
    }

    @Override
    protected void start(ServerLevel pLevel, XoonglinEntity mob, long pGameTime) {
        mob.getBrain().getMemory(CftMemoryModuleType.HOME_CONTAINER.get()).ifPresent(pos -> {
            LOGGER.trace("Xoonglin {} is moving towards supply container at {}", mob.getCustomName().getString(), pos);
            mob.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
        });
    }

    @Override
    protected void tick(ServerLevel pLevel, XoonglinEntity mob, long pGameTime) {
        if (isCloseEnoughToContainer(mob)) {
            mob.getNavigation().stop();
        }
    }

    @Override
    protected void stop(ServerLevel pLevel, XoonglinEntity mob, long pGameTime) {
        LOGGER.trace("Checking container in home");
        Optional<BlockPos> containerPos = mob.getBrain().getMemory(CftMemoryModuleType.HOME_CONTAINER.get());
        if (containerPos.isEmpty() || !isCloseEnoughToContainer(mob)) {
            mob.getBrain().setMemoryWithExpiry(CftMemoryModuleType.SUPPLY_COOLDOWN.get(), true, CftConfig.SUPPLY_COOLDOWN.get());
            return;
        }

        if (!(mob.level().getBlockEntity(containerPos.get()) instanceof Container container)) {
            mob.getBrain().eraseMemory(CftMemoryModuleType.HOME_CONTAINER.get());
            return;
        }

        Optional<List<Ingredient>> neededSupplies = mob.getBrain().getMemory(CftMemoryModuleType.SUPPLIES_NEEDED.get());
        if (neededSupplies.isEmpty()) {
            mob.getBrain().eraseMemory(CftMemoryModuleType.HOME_CONTAINER.get());
            return;
        }

        retrieveSupplies(mob, container, neededSupplies.get());

        mob.getBrain().eraseMemory(CftMemoryModuleType.SUPPLIES_NEEDED.get());
        mob.getBrain().eraseMemory(CftMemoryModuleType.HOME_CONTAINER.get());
        mob.getBrain().setMemoryWithExpiry(CftMemoryModuleType.SUPPLY_COOLDOWN.get(), true, CftConfig.SUPPLY_COOLDOWN.get());
    }

    private void retrieveSupplies(XoonglinEntity mob, Container container, List<Ingredient> neededSupplies) {
        for (Ingredient ingredient : neededSupplies) {
            int neededAmount = getNeededQuantity(mob, ingredient);
            if (neededAmount <= 0) continue;

            for (int i = 0; i < container.getContainerSize() && neededAmount > 0; i++) {
                ItemStack stack = container.getItem(i);
                if (!ingredient.test(stack) || stack.isEmpty()) continue;
                if (!mob.getInventory().canAddItem(stack)) break;

                int takenAmount = Math.min(neededAmount, stack.getCount());
                ItemStack takenStack = container.removeItem(i, takenAmount);
                mob.getInventory().addItem(takenStack);
                neededAmount -= takenAmount;
            }
        }
    }

    private int getNeededQuantity(XoonglinEntity mob, Ingredient ingredient) {
        int hoarding = findHoardingAmount(mob, ingredient);
        int currentCount = countInInventory(mob, ingredient);
        return Math.max(hoarding - currentCount, 0);
    }

    private int findHoardingAmount(XoonglinEntity mob, Ingredient ingredient) {
        if (mob.getNeeds() == null) return 1;
        for (NeedSatisfier<?> satisfier : mob.getNeeds()) {
            if (satisfier.getNeed() instanceof GoodsNeed goodsNeed
                    && goodsNeed.getIngredient().equals(ingredient)) {
                return goodsNeed.getHoarding();
            }
        }
        return 1;
    }

    private int countInInventory(XoonglinEntity mob, Ingredient ingredient) {
        int count = 0;
        for (int i = 0; i < mob.getInventory().getContainerSize(); i++) {
            ItemStack stack = mob.getInventory().getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    protected boolean canStillUse(ServerLevel pLevel, XoonglinEntity mob, long pGameTime) {
        return mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLIES_NEEDED.get())
                && mob.getBrain().hasMemoryValue(CftMemoryModuleType.HOME_CONTAINER.get())
                && !isCloseEnoughToContainer(mob)
                && !mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLY_COOLDOWN.get());
    }

    private boolean isCloseEnoughToContainer(XoonglinEntity mob) {
        return mob.getBrain()
                .getMemory(CftMemoryModuleType.HOME_CONTAINER.get()).map(
                        containerPos -> mob.position().distanceTo(containerPos.getCenter())
                                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()
                ).orElse(false);
    }
}
