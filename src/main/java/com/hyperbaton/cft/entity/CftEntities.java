package com.hyperbaton.cft.entity;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CftEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CftMod.MOD_ID);


    public static final RegistryObject<EntityType<XoonglinEntity>> XOONGLIN =
            ENTITY_TYPES.register("xoonglin", () -> EntityType.Builder.of(XoonglinEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.2f).build("xoonglin"));
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
