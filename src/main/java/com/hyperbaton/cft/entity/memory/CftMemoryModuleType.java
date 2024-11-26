package com.hyperbaton.cft.entity.memory;

import com.hyperbaton.cft.CftMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

public class CftMemoryModuleType {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_TYPES = DeferredRegister.create(Registries.MEMORY_MODULE_TYPE, CftMod.MOD_ID);
    public static final RegistryObject<MemoryModuleType<ItemStack>> SUPPLIES_NEEDED = registerMemory("supplies_needed", ItemStack.CODEC);
    public static final RegistryObject<MemoryModuleType<BlockPos>> HOME_CONTAINER_POSITION = registerMemory("home_container_position", BlockPos.CODEC);
    public static final RegistryObject<MemoryModuleType<BlockPos>> HOME_CANDIDATE_POSITION = registerMemory("home_candidate_position", BlockPos.CODEC);
    public static final RegistryObject<MemoryModuleType<Boolean>> HOME_NEEDED = registerMemory("home_needed", Codec.BOOL);
    public static final RegistryObject<MemoryModuleType<Boolean>> SUPPLY_COOLDOWN = registerMemory("supply_cooldown", Codec.BOOL);

    public static <T> RegistryObject<MemoryModuleType<T>> registerMemory(String name)
    {
        return MEMORY_TYPES.register(name, () -> new MemoryModuleType<>(Optional.empty()));
    }

    public static <T> RegistryObject<MemoryModuleType<T>> registerMemory(String name, Codec<T> codec)
    {
        return MEMORY_TYPES.register(name, () -> new MemoryModuleType<>(Optional.of(codec)));
    }
    public static void register(IEventBus eventBus){
        MEMORY_TYPES.register(eventBus);
    }
}
