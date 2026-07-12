package com.hyperbaton.cft;

import com.hyperbaton.cft.item.ManuscriptData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CftDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CftMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ManuscriptData>> MANUSCRIPT =
            DATA_COMPONENTS.registerComponentType("manuscript",
                    builder -> builder.persistent(ManuscriptData.CODEC).networkSynchronized(ManuscriptData.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
