package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.need.EnergyNeed;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.slf4j.Logger;

import java.util.Optional;

public class EnergyNeedSatisfier extends NeedSatisfier<EnergyNeed> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EnergyNeedSatisfier(double satisfaction, boolean isSatisfied, EnergyNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        int requiredEnergy = this.need.getEnergyAmount();
        LOGGER.trace("Xoonglin {} is attempting to satisfy energy need: {} FE",
                mob.getCustomName().getString(), requiredEnergy);

        // First, check if the Xoonglin already remembers an energy container
        Optional<BlockPos> rememberedContainer = mob.getBrain().getMemory(energyContainerMemoryType());

        if (rememberedContainer.isPresent()) {
            LOGGER.trace("Xoonglin {} remembers an energy container at {}",
                    mob.getCustomName().getString(), rememberedContainer.get());

            if (isCloseEnoughToContainer(mob)) {
                LOGGER.trace("Xoonglin {} is close enough to the container, attempting to extract energy.",
                        mob.getCustomName().getString());

                IEnergyStorage handler = getEnergyHandlerAt((ServerLevel) mob.level(), rememberedContainer.get());
                if (handler != null) {
                    LOGGER.trace("Energy handler found at {} for Xoonglin {}",
                            rememberedContainer.get(), mob.getCustomName().getString());

                    int simulatedExtract = handler.extractEnergy(requiredEnergy, true);
                    LOGGER.trace("Simulated extract result for {} FE: {} FE",
                            requiredEnergy, simulatedExtract);

                    if (simulatedExtract >= requiredEnergy) {
                        LOGGER.trace("Sufficient energy available, executing extraction.");
                        mob.getBrain().eraseMemory(energyContainerMemoryType());
                        return tryExtractEnergy(mob, handler, requiredEnergy);
                    } else {
                        LOGGER.trace("Not enough energy available. Removing memory of container.");
                        mob.getBrain().eraseMemory(energyContainerMemoryType());
                        mob.getBrain().setMemoryWithExpiry(CftMemoryModuleType.FLUID_SUPPLY_COOLDOWN.get(),
                                true, CftConfig.SUPPLY_COOLDOWN.get());
                        this.unsatisfy(need.getFrequency(), mob);
                        mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
                        return false;
                    }
                } else {
                    LOGGER.trace("No valid energy handler found at remembered container position. Xoonglin {} will forget it.",
                            mob.getCustomName().getString());
                    mob.getBrain().eraseMemory(energyContainerMemoryType());
                }
            } else {
                LOGGER.trace("Xoonglin {} is too far from the remembered container. Cannot extract energy.",
                        mob.getCustomName().getString());
                this.unsatisfy(need.getFrequency(), mob);
                mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
                return false;
            }
        } else {
            LOGGER.trace("Xoonglin {} does not remember an energy container. Searching for one.",
                    mob.getCustomName().getString());

            Optional<BlockPos> validEnergyContainer = findEnergyContainer(mob, requiredEnergy);
            if (validEnergyContainer.isPresent()) {
                LOGGER.trace("Found a new energy container for Xoonglin {} at {}",
                        mob.getCustomName().getString(), validEnergyContainer.get());
                mob.getBrain().setMemory(energyContainerMemoryType(), validEnergyContainer.get());
            } else {
                LOGGER.trace("No energy container found for Xoonglin {}. Need cannot be satisfied.",
                        mob.getCustomName().getString());
                mob.getBrain().eraseMemory(energyContainerMemoryType());
            }
        }

        this.unsatisfy(need.getFrequency(), mob);
        mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
        return false;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        // If the Xoonglin hasn't remembered a valid container, try finding one
        findEnergyContainer(mob, this.need.getEnergyAmount()).ifPresent(pos ->
                mob.getBrain().setMemory(energyContainerMemoryType(), pos)
        );
    }

    private Optional<BlockPos> findEnergyContainer(XoonglinEntity mob, int requiredEnergy) {
        LOGGER.trace("Searching for energy container for Xoonglin {} that can provide {} energy",
                mob.getCustomName().getString(), requiredEnergy);
        return Optional.ofNullable(mob.getHome())
                .flatMap(home -> home.getInteriorBlocks().stream()
                        .filter(pos -> {
                            IEnergyStorage handler = getEnergyHandlerAt((ServerLevel) mob.level(), pos);
                            if (handler == null) {
                                LOGGER.trace("No energy handler found at {}", pos);
                                return false;
                            }

                            LOGGER.trace("Found energy handler at {}: stored={}, maxExtract={}, canExtract={}",
                                    pos, handler.getEnergyStored(), handler.extractEnergy(requiredEnergy, true),
                                    handler.canExtract());

                            return handler.canExtract() && handler.extractEnergy(requiredEnergy, true) >= requiredEnergy;
                        })
                        .findFirst());
    }

    private IEnergyStorage getEnergyHandlerAt(ServerLevel level, BlockPos pos) {
        return Optional.ofNullable(level.getBlockEntity(pos))
                .map(blockEntity -> {
                    LOGGER.trace("Found block entity at {}: {}", pos, blockEntity.getClass().getName());
                    LazyOptional<IEnergyStorage> cap = blockEntity.getCapability(ForgeCapabilities.ENERGY);
                    if (!cap.isPresent()) {
                        LOGGER.trace("Block entity at {} does not have energy capability", pos);
                    }
                    return cap;
                })
                .flatMap(LazyOptional::resolve)
                .orElse(null);
    }

    private boolean tryExtractEnergy(XoonglinEntity mob, IEnergyStorage handler, int requiredEnergy) {
        int extracted = handler.extractEnergy(requiredEnergy, false);
        if (extracted >= requiredEnergy) {
            super.satisfy(mob);
            return true;
        }
        return false;
    }

    private boolean isCloseEnoughToContainer(XoonglinEntity mob) {
        return mob.getBrain()
                .getMemory(CftMemoryModuleType.ENERGY_CONTAINER.get())
                .map(containerPos -> mob.position().distanceTo(containerPos.getCenter())
                        < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()
                ).orElse(false);
    }

    private MemoryModuleType<BlockPos> energyContainerMemoryType() {
        return CftMemoryModuleType.ENERGY_CONTAINER.get();
    }
}