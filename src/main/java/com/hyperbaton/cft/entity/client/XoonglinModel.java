package com.hyperbaton.cft.entity.client;
// Made with Blockbench 4.9.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.hyperbaton.cft.entity.animations.XoonglinAnimations;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class XoonglinModel<T extends Entity> extends HierarchicalModel<T> {
    private final ModelPart torso;
    private final ModelPart left_shoulder;
    private final ModelPart right_shoulder;
    private final ModelPart left_hip;
    private final ModelPart left_foot;
    private final ModelPart right_hip;
    private final ModelPart right_foot;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart antenna;
    private final ModelPart right_ear;
    private final ModelPart left_ear;

    public XoonglinModel(ModelPart root) {
        this.torso = root.getChild("torso");
        this.left_shoulder = this.torso.getChild("left_shoulder");
        this.right_shoulder = this.torso.getChild("right_shoulder");
        this.left_hip = this.torso.getChild("left_hip");
        this.left_foot = this.left_hip.getChild("left_foot");
        this.right_hip = this.torso.getChild("right_hip");
        this.right_foot = this.right_hip.getChild("right_foot");
        this.body = this.torso.getChild("body");
        this.neck = this.torso.getChild("neck");
        this.head = this.neck.getChild("head");
        this.antenna = this.head.getChild("antenna");
        this.right_ear = this.head.getChild("right_ear");
        this.left_ear = this.head.getChild("left_ear");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition torso = partdefinition.addOrReplaceChild("torso", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition left_shoulder = torso.addOrReplaceChild("left_shoulder", CubeListBuilder.create(), PartPose.offset(0.0F, -3.5F, -5.0F));

        PartDefinition left_arm_r1 = left_shoulder.addOrReplaceChild("left_arm_r1", CubeListBuilder.create().texOffs(0, 44).addBox(-0.4264F, 0.5F, -4.1808F, 2.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.6109F, 0.0F));

        PartDefinition left_forearm_r1 = left_shoulder.addOrReplaceChild("left_forearm_r1", CubeListBuilder.create().texOffs(32, 38).addBox(1.0339F, 0.5F, -8.8305F, 2.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.0036F, 0.0F));

        PartDefinition right_shoulder = torso.addOrReplaceChild("right_shoulder", CubeListBuilder.create(), PartPose.offset(0.0F, -3.5F, 5.0F));

        PartDefinition right_arm_r1 = right_shoulder.addOrReplaceChild("right_arm_r1", CubeListBuilder.create().texOffs(32, 24).addBox(-1.0F, 0.5F, -1.0F, 2.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.6109F, 0.0F));

        PartDefinition right_forearm_r1 = right_shoulder.addOrReplaceChild("right_forearm_r1", CubeListBuilder.create().texOffs(32, 31).addBox(-0.9544F, -0.5F, 0.195F, 2.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.015F, 1.0F, 3.2341F, 0.0F, -1.0036F, 0.0F));

        PartDefinition left_hip = torso.addOrReplaceChild("left_hip", CubeListBuilder.create().texOffs(44, 14).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.0F, -3.0F));

        PartDefinition left_foot = left_hip.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(40, 47).addBox(0.0F, 6.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition left_toe_2_r1 = left_foot.addOrReplaceChild("left_toe_2_r1", CubeListBuilder.create().texOffs(16, 46).addBox(0.0F, -1.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.0F, 2.618F, 0.0F));

        PartDefinition left_toe_3_r1 = left_foot.addOrReplaceChild("left_toe_3_r1", CubeListBuilder.create().texOffs(28, 47).addBox(0.0F, -1.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.0F, -2.618F, 0.0F));

        PartDefinition right_hip = torso.addOrReplaceChild("right_hip", CubeListBuilder.create().texOffs(44, 5).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.0F, 3.0F));

        PartDefinition right_foot = right_hip.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(16, 44).addBox(0.0F, -1.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 7.0F, 0.0F));

        PartDefinition right_toe_2_r1 = right_foot.addOrReplaceChild("right_toe_2_r1", CubeListBuilder.create().texOffs(28, 45).addBox(0.0F, -1.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.618F, 0.0F));

        PartDefinition right_toe_3_r1 = right_foot.addOrReplaceChild("right_toe_3_r1", CubeListBuilder.create().texOffs(40, 45).addBox(0.0F, -1.0F, -0.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.618F, 0.0F));

        PartDefinition body = torso.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 24).addBox(-3.0F, -22.0F, -5.0F, 6.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 17.0F, 0.0F));

        PartDefinition neck = torso.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(44, 0).addBox(-2.0F, -0.5F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.5F, 0.0F));

        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -8.0F, -6.0F, 10.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.5F, 0.0F));

        PartDefinition antenna = head.addOrReplaceChild("antenna", CubeListBuilder.create().texOffs(48, 37).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(48, 29).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.0F, 0.0F));

        PartDefinition right_ear = head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(24, 48).addBox(1.0F, -27.0F, 4.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(16, 48).addBox(1.0F, -28.0F, 4.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 26.0F, 2.0F));

        PartDefinition left_ear = head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(48, 33).addBox(1.0F, -27.0F, 6.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(48, 23).addBox(1.0F, -28.0F, 4.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 26.0F, -14.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }


    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.applyHeadRotation(netHeadYaw, headPitch, ageInTicks);

        this.animateWalk(XoonglinAnimations.XOONGLIN_WALK, limbSwing, limbSwingAmount, 2f, 2.5f);
        this.animate(((XoonglinEntity) entity).idleAnimationState, XoonglinAnimations.XOONGLIN_IDLE, ageInTicks, 1f);
    }

    private void applyHeadRotation(float pNetHeadYaw, float pHeadPitch, float pAgeInTicks) {
        pNetHeadYaw = Mth.clamp(pNetHeadYaw, -30.0F, 30.0F);
        pHeadPitch = Mth.clamp(pHeadPitch, -25.0F, 45.0F);

        this.head.yRot = pNetHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = pHeadPitch * ((float) Math.PI / 180F);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        torso.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public ModelPart root() {
        return torso;
    }
}