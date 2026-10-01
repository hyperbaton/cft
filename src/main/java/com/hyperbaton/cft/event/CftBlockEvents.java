package com.hyperbaton.cft.event;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.util.StructureUtil;
import com.hyperbaton.cft.util.LangUtil;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * A player breaking the key block of a structure unregisters it
 * Structures broken in other ways (explosions, pistons...) are
 * unregistered when they're detected again. Runs last, so it skips breaks other mods canceled.
 */
@EventBusSubscriber(modid = CftMod.MOD_ID)
public class CftBlockEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        StructuresData.get(level).findByKeyBlock(StructureUtil.keyBlockPos(event.getState(), event.getPos()))
                .ifPresent(structure -> {
                    StructureUtil.unregister(level, structure);
                    event.getPlayer().displayClientMessage(Component.translatable("message.cft.structure_unregistered",
                            LangUtil.structureName(structure.getStructureTypeId())), true);
                });
    }
}
