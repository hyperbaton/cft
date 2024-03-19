package com.hyperbaton.cft.capability.need;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.event.CftDatapackRegistryEvents;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

import java.util.function.Supplier;

@Deprecated
/*
    TODO: Remove this class once it's clear is not useful
 */
public class CftNeeds {
    public static final DeferredRegister<GoodsNeed> NEEDS = DeferredRegister.create(CftDatapackRegistryEvents.GOODS_NEED_KEY, CftMod.MOD_ID);

    public static Supplier<IForgeRegistry<GoodsNeed>> REGISTRY;
    public static void register(IEventBus eventBus){
        NEEDS.register(eventBus);
        NEEDS.getEntries().stream().findFirst().ifPresentOrElse(need -> System.out.println("Need found:" + need.getId() + " has a value of: " + need.get().getNeedType() + "\n"),
                () -> System.out.println("No need found\n"));
        REGISTRY = NEEDS.makeRegistry(RegistryBuilder::new);
        //RegistryManager.ACTIVE.getRegistry(ForgeRegistries.Keys.ITEMS);
    }


}
