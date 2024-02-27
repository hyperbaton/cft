package com.hyperbaton.cft.item;

import com.hyperbaton.cft.CftMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CftItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CftMod.MOD_ID);

    public static final RegistryObject<Item> MAYOR_STAFF = ITEMS.register("mayor_staff",
            () -> new MayorStaff(new MayorStaff.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
