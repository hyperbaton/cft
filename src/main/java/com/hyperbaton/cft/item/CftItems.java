package com.hyperbaton.cft.item;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.CftEntities;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CftItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CftMod.MOD_ID);

    public static final RegistryObject<Item> MAYOR_STAFF = ITEMS.register("mayor_staff",
            () -> new MayorStaff(new MayorStaff.Properties().stacksTo(1)));

    public static final RegistryObject<Item> XUNGUI_SPAWN_EGG = ITEMS.register("xungui_spawn_egg",
            () -> new ForgeSpawnEggItem(CftEntities.XUNGUI, 0x121212, 0x404040, new Item.Properties()));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
