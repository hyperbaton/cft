package com.hyperbaton.cft.creativetab;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.item.CftItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CftMod.MOD_ID);

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }

    public static final RegistryObject<CreativeModeTab> TUTORIAL_TAB = CREATIVE_MODE_TABS.register("cft",
            () -> CreativeModeTab.builder().icon((() -> new ItemStack(CftItems.LEADER_STAFF.get())))
                    .title(Component.translatable("creativetab.tutorial_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(CftItems.LEADER_STAFF.get());
                        pOutput.accept(CftItems.XOONGLIN_SPAWN_EGG.get());
                    })
                    .build()
    );
}
