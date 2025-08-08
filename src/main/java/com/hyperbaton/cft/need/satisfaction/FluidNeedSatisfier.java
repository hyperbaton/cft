package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.need.FluidNeed;
import com.hyperbaton.cft.structure.home.XoonglinHome;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.slf4j.Logger;

import java.util.Optional;

public class FluidNeedSatisfier extends NeedSatisfier<FluidNeed> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public FluidNeedSatisfier(double satisfaction, boolean isSatisfied, FluidNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        FluidStack requiredFluid = this.need.getFluidStack();
        LOGGER.trace("Xoonglin {} is attempting to satisfy fluid need: {} x{}", mob.getCustomName().getString(), requiredFluid.getFluid().getFluidType().getDescriptionId(), requiredFluid.getAmount());

        // First, check if the Xoonglin already remembers a fluid container
        Optional<BlockPos> rememberedContainer = mob.getBrain().getMemory(fluidContainerMemoryType());

        if (rememberedContainer.isPresent()) {
            LOGGER.trace("Xoonglin {} remembers a fluid container at {}", mob.getCustomName().getString(), rememberedContainer.get());

            if (isCloseEnoughToContainer(mob)) {
                LOGGER.trace("Xoonglin {} is close enough to the container, attempting to retrieve fluid.", mob.getCustomName().getString());

                IFluidHandler handler = getFluidHandlerAt((ServerLevel) mob.level(), rememberedContainer.get());
                if (handler != null) {
                    LOGGER.trace("Fluid handler found at {} for Xoonglin {}", rememberedContainer.get(), mob.getCustomName().getString());

                    FluidStack drainSimulated = handler.drain(requiredFluid, IFluidHandler.FluidAction.SIMULATE);
                    LOGGER.trace("Simulated drain result for {} x{}: {} x{}", requiredFluid.getFluid().getFluidType().getDescriptionId(),
                            requiredFluid.getAmount(),
                            drainSimulated.getFluid().getFluidType().getDescriptionId(),
                            drainSimulated.getAmount());

                    if (drainSimulated.isFluidEqual(requiredFluid) && drainSimulated.getAmount() >= requiredFluid.getAmount()) {
                        LOGGER.trace("Sufficient fluid available, executing drain.");
                        mob.getBrain().eraseMemory(fluidContainerMemoryType());
                        return tryDrainFluid(mob, handler, requiredFluid);
                    } else {
                        LOGGER.trace("Not enough fluid available. Removing memory of container.");
                        mob.getBrain().eraseMemory(fluidContainerMemoryType());
                        mob.getBrain().setMemoryWithExpiry(CftMemoryModuleType.FLUID_SUPPLY_COOLDOWN.get(), true, CftConfig.SUPPLY_COOLDOWN.get());
                        this.unsatisfy(need.getFrequency(), mob);
                        mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
                        return false;
                    }
                } else {
                    LOGGER.trace("No valid fluid handler found at remembered container position. Xoonglin {} will forget it.", mob.getCustomName().getString());
                    mob.getBrain().eraseMemory(fluidContainerMemoryType());
                }
            } else {
                LOGGER.trace("Xoonglin {} is too far from the remembered container. Cannot retrieve fluid.", mob.getCustomName().getString());
                this.unsatisfy(need.getFrequency(), mob);
                mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
                return false;
            }
        } else {
            LOGGER.trace("Xoonglin {} does not remember a fluid container. Searching for one.", mob.getCustomName().getString());

            Optional<BlockPos> validFluidContainer = findFluidContainer(mob, requiredFluid);
            if (validFluidContainer.isPresent()) {
                LOGGER.trace("Found a new fluid container for Xoonglin {} at {}", mob.getCustomName().getString(), validFluidContainer.get());
                mob.getBrain().setMemory(fluidContainerMemoryType(), validFluidContainer.get());
            } else {
                LOGGER.trace("No fluid container found for Xoonglin {}. Need cannot be satisfied.", mob.getCustomName().getString());
                mob.getBrain().eraseMemory(fluidContainerMemoryType());
            }
        }

        this.unsatisfy(need.getFrequency(), mob);
        mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
        return false;
    }


    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        // If the Xoonglin hasn't remembered a valid container, try finding one
        findFluidContainer(mob, this.need.getFluidStack()).ifPresent(pos ->
                mob.getBrain().setMemory(fluidContainerMemoryType(), pos)
        );
    }

    private Optional<BlockPos> findFluidContainer(XoonglinEntity mob, FluidStack requiredFluid) {
        return Optional.ofNullable(mob.getHome())
                .flatMap(home -> home.getInteriorBlocks().stream()
                        .filter(pos -> {
                            IFluidHandler handler = getFluidHandlerAt((ServerLevel) mob.level(), pos);
                            return handler != null && handler.drain(requiredFluid, IFluidHandler.FluidAction.SIMULATE).isFluidEqual(requiredFluid);
                        })
                        .findFirst());
    }

    private IFluidHandler getFluidHandlerAt(ServerLevel level, BlockPos pos) {
        return Optional.ofNullable(level.getBlockEntity(pos))
                .map(blockEntity -> blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER))
                .flatMap(LazyOptional::resolve)
                .orElse(null);
    }

    private boolean tryDrainFluid(XoonglinEntity mob, IFluidHandler handler, FluidStack requiredFluid) {
        FluidStack drained = handler.drain(requiredFluid, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isFluidEqual(requiredFluid) && drained.getAmount() >= requiredFluid.getAmount()) {
            super.satisfy(mob);
            return true;
        }
        return false;
    }

    private boolean isCloseEnoughToContainer(XoonglinEntity mob) {
        return mob.getBrain()
                .getMemory(CftMemoryModuleType.FLUID_CONTAINER.get()).map(
                        containerPos -> mob.position().distanceTo(containerPos.getCenter())
                                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()
                ).orElse(false);
    }

    private MemoryModuleType<BlockPos> fluidContainerMemoryType() {
        return CftMemoryModuleType.FLUID_CONTAINER.get();
    }
}
