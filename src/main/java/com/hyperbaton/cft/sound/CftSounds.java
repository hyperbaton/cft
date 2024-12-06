package com.hyperbaton.cft.sound;

import com.hyperbaton.cft.CftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CftSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CftMod.MOD_ID);

    public static final RegistryObject<SoundEvent> XOONGLIN_HURT = registerSoundEvents("entity.xoonglin.hurt");
    public static final RegistryObject<SoundEvent> XOONGLIN_DEATH = registerSoundEvents("entity.xoonglin.death");
    public static final RegistryObject<SoundEvent> XOONGLIN_AMBIENT = registerSoundEvents("entity.xoonglin.ambient");

    private static RegistryObject<SoundEvent> registerSoundEvents(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CftMod.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
