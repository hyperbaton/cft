package com.hyperbaton.cft.entity.client;

import com.hyperbaton.cft.CftClientConfig;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.item.CftItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Draws the icon of a Xoonglin's most pressing need above its head, so a leader can
 * spot problems among its Xoonglins without inspecting each Xoonglin with the staff.
 */
public class NeedIndicatorRenderer {
    /** Height of the icon above the name tag, in the renderer's (model-scaled) space. */
    private static final float GAP_ABOVE_NAME_TAG = 0.45F;
    /** Icon size in world blocks, independent of the model scale. */
    private static final float ICON_WORLD_SIZE = 0.4F;
    private static final int UNSATISFIED_COLOR = 0xFFFFD000;
    private static final int CRITICAL_COLOR = 0xFFFF3030;

    public static boolean shouldShow(XoonglinEntity entity) {
        byte alert = entity.getNeedAlert();
        if (alert == XoonglinEntity.ALERT_NONE || entity.isInvisible()) return false;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !entity.getSyncedLeaderId().map(player.getUUID()::equals).orElse(false)) {
            return false;
        }

        boolean holdingStaff = player.isHolding(CftItems.LEADER_STAFF.get());
        return switch (CftClientConfig.NEED_INDICATOR.get()) {
            case ALL -> true;
            case CRITICAL -> alert == XoonglinEntity.ALERT_CRITICAL || holdingStaff;
            case STAFF_ONLY -> holdingStaff;
            case NONE -> false;
        };
    }

    /**
     * @param nameTagY   height of the name tag in the current pose space
     * @param modelScale scale already applied to the pose stack by the entity renderer
     */
    public static void render(XoonglinEntity entity, float nameTagY, float modelScale, float partialTicks,
                              PoseStack poseStack, MultiBufferSource buffer, EntityRenderDispatcher dispatcher,
                              Font font) {
        boolean critical = entity.getNeedAlert() == XoonglinEntity.ALERT_CRITICAL;
        float iconScale = ICON_WORLD_SIZE / modelScale;
        if (critical) {
            float pulse = Mth.sin((entity.tickCount + partialTicks) * 0.25F) * 0.08F;
            iconScale *= 1.0F + pulse;
        }

        poseStack.pushPose();
        poseStack.translate(0, nameTagY + GAP_ABOVE_NAME_TAG / modelScale, 0);
        poseStack.mulPose(dispatcher.cameraOrientation());

        poseStack.pushPose();
        poseStack.scale(iconScale, iconScale, iconScale);
        Minecraft.getInstance().getItemRenderer().renderStatic(getIcon(entity), ItemDisplayContext.GUI,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();

        // Exclamation mark on the left of the icon, coloured by severity
        float textScale = 0.035F / modelScale;
        poseStack.translate(-iconScale * 0.75F, iconScale * 0.25F, 0);
        poseStack.scale(textScale, -textScale, textScale);
        String mark = "!";
        font.drawInBatch(mark, -font.width(mark) / 2.0F, 0, critical ? CRITICAL_COLOR : UNSATISFIED_COLOR, true,
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }

    private static ItemStack getIcon(XoonglinEntity entity) {
        String iconId = entity.getNeedAlertIcon();
        if (!iconId.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(iconId);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                return new ItemStack(BuiltInRegistries.ITEM.get(id));
            }
        }
        return new ItemStack(Items.BARRIER);
    }
}
