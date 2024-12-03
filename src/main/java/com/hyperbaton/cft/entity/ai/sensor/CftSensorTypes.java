package com.hyperbaton.cft.entity.ai.sensor;

import com.hyperbaton.cft.CftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class CftSensorTypes {
    public static final DeferredRegister<SensorType<?>> SENSOR_TYPES = DeferredRegister.create(Registries.SENSOR_TYPE, CftMod.MOD_ID);
    public static final RegistryObject<SensorType<AbleToMateSensor>> ABLE_TO_MATE = registerSensorType("able_to_mate", AbleToMateSensor::new);
    public static final RegistryObject<SensorType<FindPotentialMatesSensor>> FIND_POTENTIAL_MATES = registerSensorType("find_potential_mates", FindPotentialMatesSensor::new);

    public static <T extends Sensor<?>> RegistryObject<SensorType<T>> registerSensorType(String name, Supplier<T> supplier)
    {
        return SENSOR_TYPES.register(name, () -> new SensorType<>(supplier));
    }
    public static void register(IEventBus eventBus){
        SENSOR_TYPES.register(eventBus);
    }
}
