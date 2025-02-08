package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.FluidNeed;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FluidNeedSatisfier extends NeedSatisfier<FluidNeed> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public FluidNeedSatisfier(double satisfaction, boolean isSatisfied, FluidNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        FluidStack requiredFluid = this.need.getFluidStack();

        Optional<IFluidHandler> validFluidHandler = getNearbyFluidHandlers(mob).stream()
                .filter(fluidHandler -> {
                    FluidStack drained = fluidHandler.drain(requiredFluid, IFluidHandler.FluidAction.SIMULATE);
                    return drained.isFluidEqual(requiredFluid) && drained.getAmount() >= requiredFluid.getAmount();
                })
                .findFirst();

        if (validFluidHandler.isPresent()) {
            validFluidHandler.get().drain(requiredFluid, IFluidHandler.FluidAction.EXECUTE);
            super.satisfy(mob);
            return true;
        } else {
            this.unsatisfy(need.getFrequency(), mob);
            mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
            return false;
        }
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {

    }

    private List<IFluidHandler> getNearbyFluidHandlers(XoonglinEntity mob) {
        return mob.getHome().getInteriorBlocks().stream()
                .map(testPos -> mob.level().getBlockEntity(testPos))
                .filter(Objects::nonNull)
                .map(blockEntity -> blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER))
                .map(LazyOptional::resolve)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
