package com.hyperbaton.cft.sound;

import com.hyperbaton.cft.CftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CftSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, CftMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> XOONGLIN_HURT = registerSoundEvents("entity.xoonglin.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> XOONGLIN_DEATH = registerSoundEvents("entity.xoonglin.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> XOONGLIN_AMBIENT = registerSoundEvents("entity.xoonglin.ambient");

    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvents(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
