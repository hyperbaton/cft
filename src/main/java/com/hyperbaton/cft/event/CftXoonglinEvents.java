package com.hyperbaton.cft.event;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.api.event.SocialClassChangeEvent;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.ClassChangeNotificationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

/** CFT's own reactions to what happens to Xoonglins. */
@EventBusSubscriber(modid = CftMod.MOD_ID)
public class CftXoonglinEvents {

    /** Tells the leader, if they're online, that one of their Xoonglins changed class. */
    @SubscribeEvent
    public static void onSocialClassChange(SocialClassChangeEvent.Post event) {
        XoonglinEntity xoonglin = event.getXoonglin();
        if (xoonglin.getLeaderId() != null
                && xoonglin.level().getPlayerByUUID(xoonglin.getLeaderId()) instanceof ServerPlayer leader) {
            PacketDistributor.sendToPlayer(leader, new ClassChangeNotificationPacket(xoonglin.getName().getString(),
                    event.getPreviousClass(), event.getNewClass(), event.isUpgrade()));
        }
    }
}
