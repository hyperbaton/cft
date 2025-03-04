package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class GetFluidBehavior extends Behavior<XoonglinEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public GetFluidBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity mob) {
        boolean hasContainerMemory = mob.getBrain().getMemory(fluidContainerMemoryType()).isPresent();
        boolean isOnCooldown = mob.getBrain().hasMemoryValue(fluidSupplyCooldownMemoryType());

        LOGGER.trace("Checking GetFluidBehavior start conditions for Xoonglin {}: Has container memory? {} | On cooldown? {}",
                mob.getCustomName().getString(), hasContainerMemory, isOnCooldown);

        return hasContainerMemory && !isOnCooldown;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity mob, long gameTime) {
        Optional<BlockPos> fluidContainerPos = mob.getBrain().getMemory(fluidContainerMemoryType());

        fluidContainerPos.ifPresentOrElse(pos -> {
            LOGGER.trace("Xoonglin {} is moving towards fluid container at {}", mob.getCustomName().getString(), pos);
            mob.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
        }, () -> LOGGER.trace("Xoonglin {} tried to start GetFluidBehavior, but no fluid container was found in memory!", mob.getCustomName().getString()));
    }

    @Override
    protected void tick(ServerLevel pLevel, XoonglinEntity mob, long pGameTime) {
        if (isCloseEnoughToContainer(mob)) {
            LOGGER.trace("Xoonglin {} reached fluid container, stopping movement.", mob.getCustomName().getString());
            mob.getNavigation().stop(); // Stop moving once close
        } else {
            LOGGER.trace("Xoonglin {} is still moving towards fluid container.", mob.getCustomName().getString());
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel pLevel, XoonglinEntity mob, long pGameTime) {
        boolean hasContainerMemory = mob.getBrain().hasMemoryValue(fluidContainerMemoryType());
        boolean isCloseEnough = isCloseEnoughToContainer(mob);
        boolean isOnCooldown = mob.getBrain().hasMemoryValue(fluidSupplyCooldownMemoryType());

        LOGGER.trace("Checking if Xoonglin {} can still use GetFluidBehavior: Has container? {} | Close enough? {} | On cooldown? {}",
                mob.getUUID(), hasContainerMemory, isCloseEnough, isOnCooldown);

        return hasContainerMemory && !isCloseEnough && !isOnCooldown;
    }

    private boolean isCloseEnoughToContainer(XoonglinEntity mob) {
        return mob.getBrain()
                .getMemory(fluidContainerMemoryType()).map(
                        containerPos -> {
                            double distance = mob.position().distanceTo(containerPos.getCenter());
                            boolean closeEnough = distance < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get();
                            LOGGER.trace("Xoonglin {} is {} blocks away from fluid container. Close enough? {}", mob.getCustomName().getString(), distance, closeEnough);
                            return closeEnough;
                        }
                ).orElse(false);
    }

    @Override
    protected void stop(ServerLevel pLevel, XoonglinEntity mob, long pGameTime) {
        LOGGER.trace("Xoonglin {} finished fluid retrieval behavior, setting cooldown.", mob.getCustomName().getString());
        mob.getBrain().setMemoryWithExpiry(fluidSupplyCooldownMemoryType(), true, CftConfig.SUPPLY_COOLDOWN.get());
    }

    private MemoryModuleType<BlockPos> fluidContainerMemoryType() {
        return CftMemoryModuleType.FLUID_CONTAINER.get();
    }
    private @NotNull MemoryModuleType<Boolean> fluidSupplyCooldownMemoryType() {
        return CftMemoryModuleType.FLUID_SUPPLY_COOLDOWN.get();
    }
}

