package com.hyperbaton.cft.entity.ai.sensor;

import com.hyperbaton.cft.CftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class CftSensorTypes {
    public static final DeferredRegister<SensorType<?>> SENSOR_TYPES = DeferredRegister.create(Registries.SENSOR_TYPE, CftMod.MOD_ID);
    public static final DeferredHolder<SensorType<?>, SensorType<AbleToMateSensor>> ABLE_TO_MATE = registerSensorType("able_to_mate", AbleToMateSensor::new);
    public static final DeferredHolder<SensorType<?>, SensorType<FindPotentialMatesSensor>> FIND_POTENTIAL_MATES = registerSensorType("find_potential_mates", FindPotentialMatesSensor::new);

    public static <T extends Sensor<?>> DeferredHolder<SensorType<?>, SensorType<T>> registerSensorType(String name, Supplier<T> supplier)
    {
        return SENSOR_TYPES.register(name, () -> new SensorType<>(supplier));
    }
    public static void register(IEventBus eventBus){
        SENSOR_TYPES.register(eventBus);
    }
}
