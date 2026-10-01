package com.hyperbaton.cft.entity.client;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class XoonglinRenderer extends MobRenderer<XoonglinEntity, EntityModel<XoonglinEntity>> {
    /** Height of XoonglinModel from the feet to the tip of the antenna (34 px), before scaling. */
    private static final float CUSTOM_MODEL_HEIGHT = 34.0F / 16.0F;
    /** Space vanilla leaves between the name tag attachment point and the name tag. */
    private static final float NAME_TAG_GAP = 0.5F;
    /** For Xoonglins whose class has no texture of its own, or that have no class yet. */
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "textures/entity/xoonglin.png");

    private final Map<String, ResourceLocation> classTextures = new HashMap<>();

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
        return classTextures.computeIfAbsent(xoonglinEntity.getEntityData().get(XoonglinEntity.SOCIAL_CLASS_NAME),
                XoonglinRenderer::classTexture);
    }

    /**
     * A class's texture is in the class's own namespace, at {@code textures/entity/<path>.png}
     */
    private static ResourceLocation classTexture(String classId) {
        ResourceLocation id = ResourceLocation.tryParse(classId);
        if (id == null || id.getPath().isEmpty()) return DEFAULT_TEXTURE;
        ResourceLocation texture = id.withPath(path -> "textures/entity/" + path + ".png");
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent() ? texture : DEFAULT_TEXTURE;
    }

    @Override
    public void render(XoonglinEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        // Switch model based on config
        if (CftConfig.USE_HUMANOID_MODEL.get()) {
            this.model = humanoidModel;
        } else {
            this.model = customModel;
        }

        float modelScale;
        if (CftConfig.USE_HUMANOID_MODEL.get()) {
            // HumanoidModel already renders babies with baby proportions, so it must
            // not be shrunk again here.
            modelScale = 1.0f;
        } else if (pEntity.isBaby()) {
            modelScale = 0.4f;
        } else {
            modelScale = 0.75f;
        }
        pMatrixStack.scale(modelScale, modelScale, modelScale);
        //pMatrixStack.rotateAround(new Quaternionf(0f, 0f, 0f, 0f), 0f, 0f, 0f);

        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);

        if (NeedIndicatorRenderer.shouldShow(pEntity)) {
            NeedIndicatorRenderer.render(pEntity, getNameTagY(pEntity, pPartialTicks), modelScale, pPartialTicks,
                    pMatrixStack, pBuffer, this.entityRenderDispatcher, this.getFont());
        }
    }

    @Override
    protected void renderNameTag(XoonglinEntity entity, Component name, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTick) {
        poseStack.pushPose();
        poseStack.translate(0, getNameTagY(entity, partialTick) - getVanillaNameTagY(entity, partialTick), 0);
        super.renderNameTag(entity, name, poseStack, buffer, packedLight, partialTick);
        poseStack.popPose();
    }

    /**
     * Height of the name tag in the renderer's (model-scaled) space.
     */
    private static float getNameTagY(XoonglinEntity entity, float partialTick) {
        return CftConfig.USE_HUMANOID_MODEL.get()
                ? getVanillaNameTagY(entity, partialTick)
                : CUSTOM_MODEL_HEIGHT + NAME_TAG_GAP;
    }

    /** Where {@code EntityRenderer#renderNameTag} draws the name tag by itself. */
    private static float getVanillaNameTagY(XoonglinEntity entity, float partialTick) {
        Vec3 attachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
        float baseY = attachment != null ? (float) attachment.y : entity.getBbHeight();
        return baseY + NAME_TAG_GAP;
    }

}
