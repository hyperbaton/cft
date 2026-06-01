package com.hyperbaton.cft.event;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = CftMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CftEventBusEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(CftEntities.XOONGLIN.get(), XoonglinEntity.createAttributes().build());
    }
}
