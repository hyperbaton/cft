package com.hyperbaton.cft.entity.client;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class XoonglinRenderer extends MobRenderer<XoonglinEntity, XoonglinModel<XoonglinEntity>> {
    public XoonglinRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new XoonglinModel<>(pContext.bakeLayer(CftModelLayers.XOONGLIN_LAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(XoonglinEntity xoonglinEntity) {
        return new ResourceLocation(CftMod.MOD_ID, "textures/entity/"
                + xoonglinEntity.getEntityData().get(XoonglinEntity.SOCIAL_CLASS_NAME).replaceFirst("(.*?):", "")
                + ".png");
    }

    @Override
    public void render(XoonglinEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        if (pEntity.isBaby()) {
            pMatrixStack.scale(0.4f, 0.4f, 0.4f);
        } else {
            pMatrixStack.scale(0.75f, 0.75f, 0.75f);
        }
        //pMatrixStack.rotateAround(new Quaternionf(0f, 0f, 0f, 0f), 0f, 0f, 0f);

        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }
}
