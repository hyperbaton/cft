package com.hyperbaton.cft.entity.ai.activity;

import com.hyperbaton.cft.CftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CftActivities {

    public static final DeferredRegister<Activity> ACTIVITIES = DeferredRegister.create(Registries.ACTIVITY, CftMod.MOD_ID);

    public static final RegistryObject<Activity> MATE = registerActivity("mate");

    public static RegistryObject<Activity> registerActivity(String name)
    {
        return ACTIVITIES.register(name, () -> new Activity(name));
    }
    public static void register(IEventBus eventBus){
        ACTIVITIES.register(eventBus);
    }
}
