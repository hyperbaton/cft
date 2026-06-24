package com.hyperbaton.cft.event;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.client.gui.socialclass.SocialClassBrowserScreen;
import com.hyperbaton.cft.client.keybind.CftKeyBindings;
import com.hyperbaton.cft.client.render.StructureLabelRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = CftMod.MOD_ID, value = Dist.CLIENT)
public class CftClientForgeEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (CftKeyBindings.OPEN_SOCIAL_CLASS_BROWSER.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.screen == null) {
                mc.setScreen(new SocialClassBrowserScreen());
            }
        }

        StructureLabelRenderer.onClientTick();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        StructureLabelRenderer.onRenderLevel(event.getPoseStack(), event.getCamera());
    }
}
