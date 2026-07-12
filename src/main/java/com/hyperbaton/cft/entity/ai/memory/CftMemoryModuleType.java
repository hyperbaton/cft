package com.hyperbaton.cft.entity.ai.memory;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.need.codec.CftCodec;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

public class CftMemoryModuleType {
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_TYPES = DeferredRegister.create(Registries.MEMORY_MODULE_TYPE, CftMod.MOD_ID);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<List<Ingredient>>> SUPPLIES_NEEDED = registerMemory("supplies_needed", CftCodec.INGREDIENT_CODEC.listOf());
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> HOME_CONTAINER = registerMemory("home_container", BlockPos.CODEC);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> HOME_CANDIDATE_POSITION = registerMemory("home_candidate_position", BlockPos.CODEC);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> HOME_NEEDED = registerMemory("home_needed", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> SUPPLY_COOLDOWN = registerMemory("supply_cooldown", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> CAN_MATE = registerMemory("can_mate", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<String>> MATING_CANDIDATE = registerMemory("mating_candidate", Codec.STRING);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> FLUID_CONTAINER = registerMemory("fluid_container", BlockPos.CODEC);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> FLUID_SUPPLY_COOLDOWN = registerMemory("fluid_supply_cooldown", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> ENERGY_CONTAINER = registerMemory("energy_container", BlockPos.CODEC);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> ENERGY_SUPPLY_COOLDOWN = registerMemory("energy_supply_cooldown", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_WORK_AT_HOME = registerMemory("must_work_at_home", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_GATHER = registerMemory("must_gather", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_GUARD = registerMemory("must_guard", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_FARM = registerMemory("must_farm", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_HAUL = registerMemory("must_haul", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_BUILD = registerMemory("must_build", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_PERFORM_RITUAL = registerMemory("must_perform_ritual", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_CRAFT = registerMemory("must_craft", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_FISH = registerMemory("must_fish", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_HEAL = registerMemory("must_heal", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_BLESS = registerMemory("must_bless", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_MINE = registerMemory("must_mine", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_ENCHANT = registerMemory("must_enchant", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_RANCH = registerMemory("must_ranch", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_WRITE = registerMemory("must_write", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> MUST_SCRIBE = registerMemory("must_scribe", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> QUARRY_FLOODED = registerMemory("quarry_flooded", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Boolean>> QUARRY_NEEDS_LADDERS = registerMemory("quarry_needs_ladders", Codec.BOOL);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> MUST_ATTEND_RITUAL = registerMemory("must_attend_ritual", BlockPos.CODEC);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<String>> STRUCTURE_NEEDED = registerMemory("structure_needed", Codec.STRING);
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<BlockPos>> STRUCTURE_CANDIDATE_POSITION = registerMemory("structure_candidate_position", BlockPos.CODEC);

    public static <T> DeferredHolder<MemoryModuleType<?>, MemoryModuleType<T>> registerMemory(String name)
    {
        return MEMORY_TYPES.register(name, () -> new MemoryModuleType<>(Optional.empty()));
    }

    public static <T> DeferredHolder<MemoryModuleType<?>, MemoryModuleType<T>> registerMemory(String name, Codec<T> codec)
    {
        return MEMORY_TYPES.register(name, () -> new MemoryModuleType<>(Optional.of(codec)));
    }
    public static void register(IEventBus eventBus){
        MEMORY_TYPES.register(eventBus);
    }
}
