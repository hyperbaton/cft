package com.hyperbaton.cft.client.render;

import com.hyperbaton.cft.item.CftItems;
import com.hyperbaton.cft.network.RequestStructureLabelsPacket;
import com.hyperbaton.cft.network.StructureLabelsPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

public class StructureLabelRenderer {

    private static final int REQUEST_INTERVAL = 40;
    private static final double MAX_RENDER_DISTANCE_SQ = 64.0 * 64.0;
    private static final float LABEL_SCALE = 0.025f;
    private static final int LABEL_BG_COLOR = 0x80000000;
    private static final int LABEL_TEXT_COLOR = 0xFF55FF55;

    private static List<StructureLabelsPacket.Entry> labels = Collections.emptyList();
    private static int requestTimer = 0;
    private static boolean wasHoldingStaff = false;

    public static void updateLabels(List<StructureLabelsPacket.Entry> newLabels) {
        labels = newLabels;
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            labels = Collections.emptyList();
            return;
        }

        boolean holdingStaff = mc.player.getMainHandItem().is(CftItems.LEADER_STAFF.get())
                || mc.player.getOffhandItem().is(CftItems.LEADER_STAFF.get());

        if (!holdingStaff) {
            if (wasHoldingStaff) {
                labels = Collections.emptyList();
            }
            wasHoldingStaff = false;
            requestTimer = 0;
            return;
        }

        wasHoldingStaff = true;
        if (++requestTimer >= REQUEST_INTERVAL || requestTimer == 1) {
            PacketDistributor.sendToServer(new RequestStructureLabelsPacket());
            if (requestTimer >= REQUEST_INTERVAL) requestTimer = 1;
        }
    }

    public static void onRenderLevel(PoseStack poseStack, net.minecraft.client.Camera camera) {
        if (labels.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Vec3 cameraPos = camera.getPosition();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        for (StructureLabelsPacket.Entry entry : labels) {
            double dx = entry.pos().getX() + 0.5 - cameraPos.x;
            double dy = entry.pos().getY() + 2.2 - cameraPos.y;
            double dz = entry.pos().getZ() + 0.5 - cameraPos.z;

            if (dx * dx + dy * dy + dz * dz > MAX_RENDER_DISTANCE_SQ) continue;

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

            Matrix4f matrix = poseStack.last().pose();
            float halfWidth = font.width(entry.label()) / 2.0f;

            font.drawInBatch(entry.label(), -halfWidth, 0, LABEL_TEXT_COLOR, false,
                    matrix, bufferSource, Font.DisplayMode.NORMAL, LABEL_BG_COLOR,
                    LightTexture.FULL_BRIGHT);
            font.drawInBatch(entry.label(), -halfWidth, 0, 0x20FFFFFF, false,
                    matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, LABEL_BG_COLOR,
                    LightTexture.FULL_BRIGHT);

            poseStack.popPose();
        }

        bufferSource.endBatch();
    }
}
