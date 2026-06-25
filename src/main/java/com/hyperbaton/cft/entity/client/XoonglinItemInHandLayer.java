package com.hyperbaton.cft.entity.client;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class XoonglinItemInHandLayer extends RenderLayer<XoonglinEntity, EntityModel<XoonglinEntity>> {
    private final ItemInHandRenderer itemInHandRenderer;

    public XoonglinItemInHandLayer(RenderLayerParent<XoonglinEntity, EntityModel<XoonglinEntity>> renderer,
                                   ItemInHandRenderer itemInHandRenderer) {
        super(renderer);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       XoonglinEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack mainHandItem = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack offHandItem = entity.getItemBySlot(EquipmentSlot.OFFHAND);

        if (!mainHandItem.isEmpty()) {
            renderArmWithItem(entity, mainHandItem, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    HumanoidArm.RIGHT, poseStack, buffer, packedLight);
        }
        if (!offHandItem.isEmpty()) {
            renderArmWithItem(entity, offHandItem, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    HumanoidArm.LEFT, poseStack, buffer, packedLight);
        }
    }

    private void renderArmWithItem(XoonglinEntity entity, ItemStack itemStack,
                                   ItemDisplayContext displayContext, HumanoidArm arm,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!(this.getParentModel() instanceof ArmedModel armedModel)) return;

        poseStack.pushPose();
        armedModel.translateToHand(arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        boolean isLeftHand = arm == HumanoidArm.LEFT;
        poseStack.translate((isLeftHand ? -1 : 1) / 16.0F, 0.125F, -0.625F);
        this.itemInHandRenderer.renderItem(entity, itemStack, displayContext, isLeftHand, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
