package com.hyperbaton.cft.entity.ai.activity;

import com.hyperbaton.cft.CftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CftActivities {

    public static final DeferredRegister<Activity> ACTIVITIES = DeferredRegister.create(Registries.ACTIVITY, CftMod.MOD_ID);

    public static final DeferredHolder<Activity, Activity> MATE = registerActivity("mate");

    public static DeferredHolder<Activity, Activity> registerActivity(String name)
    {
        return ACTIVITIES.register(name, () -> new Activity(name));
    }
    public static void register(IEventBus eventBus){
        ACTIVITIES.register(eventBus);
    }
}
