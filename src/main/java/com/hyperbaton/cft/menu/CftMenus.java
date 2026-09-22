package com.hyperbaton.cft.menu;

import com.hyperbaton.cft.CftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CftMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CftMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<TradeConfigMenu>> TRADE_CONFIG =
            MENUS.register("trade_config", () -> IMenuTypeExtension.create(TradeConfigMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
