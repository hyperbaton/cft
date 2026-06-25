package com.hyperbaton.cft.entity.client;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class XoonglinRenderer extends MobRenderer<XoonglinEntity, EntityModel<XoonglinEntity>> {
    private final XoonglinModel<XoonglinEntity> customModel;
    private final HumanoidModel<XoonglinEntity> humanoidModel;

    public XoonglinRenderer(EntityRendererProvider.Context pContext) {
        super(pContext,
              CftConfig.USE_HUMANOID_MODEL.get()
                  ? new HumanoidModel<>(pContext.bakeLayer(ModelLayers.PLAYER))
                  : new XoonglinModel<>(pContext.bakeLayer(CftModelLayers.XOONGLIN_LAYER)),
              0.5f);
        this.customModel = new XoonglinModel<>(pContext.bakeLayer(CftModelLayers.XOONGLIN_LAYER));
        this.humanoidModel = new HumanoidModel<>(pContext.bakeLayer(ModelLayers.PLAYER));
        this.addLayer(new XoonglinItemInHandLayer(this, pContext.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(XoonglinEntity xoonglinEntity) {
        return ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "textures/entity/"
                + xoonglinEntity.getEntityData().get(XoonglinEntity.SOCIAL_CLASS_NAME).replaceFirst("(.*?):", "")
                + ".png");
    }

    @Override
    public void render(XoonglinEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        // Switch model based on config
        if (CftConfig.USE_HUMANOID_MODEL.get()) {
            this.model = humanoidModel;
        } else {
            this.model = customModel;
        }

        if (pEntity.isBaby()) {
            pMatrixStack.scale(0.4f, 0.4f, 0.4f);
        } else {
            if (CftConfig.USE_HUMANOID_MODEL.get()) {
                pMatrixStack.scale(1.0f, 1.0f, 1.0f);
            } else {
                pMatrixStack.scale(0.75f, 0.75f, 0.75f);
            }
        }
        //pMatrixStack.rotateAround(new Quaternionf(0f, 0f, 0f, 0f), 0f, 0f, 0f);

        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    @Override
    protected void renderNameTag(XoonglinEntity entity, Component name, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTick) {
        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * 0.6F, 0);
        super.renderNameTag(entity, name, poseStack, buffer, packedLight, partialTick);
        poseStack.popPose();
    }

}
