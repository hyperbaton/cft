package com.hyperbaton.cft.item;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.CftEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CftItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, CftMod.MOD_ID);

    public static final DeferredHolder<Item, Item> LEADER_STAFF = ITEMS.register("leader_staff",
            () -> new LeaderStaff(new LeaderStaff.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> XOONGLIN_SPAWN_EGG = ITEMS.register("xoonglin_spawn_egg",
            () -> new DeferredSpawnEggItem(CftEntities.XOONGLIN, 0x121212, 0x404040, new Item.Properties()));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
