package com.hyperbaton.cft.entity;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CftEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CftMod.MOD_ID);


    public static final DeferredHolder<EntityType<?>, EntityType<XoonglinEntity>> XOONGLIN =
            ENTITY_TYPES.register("xoonglin", () -> EntityType.Builder.of(XoonglinEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.2f).build(CftMod.MOD_ID + ":xoonglin"));
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
