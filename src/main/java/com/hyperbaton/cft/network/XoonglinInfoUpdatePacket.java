package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.client.gui.XoonglinInfoScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.core.UUIDUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record XoonglinInfoUpdatePacket(
        Component name,
        String socialClass,
        ResourceLocation jobId,
        double happiness,
        Map<String, NeedSatisfactionData> needsData,
        UUID xoonglinId
) implements CustomPacketPayload {

    public static final Type<XoonglinInfoUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "xoonglin_info_update"));

    public static final StreamCodec<ByteBuf, XoonglinInfoUpdatePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public XoonglinInfoUpdatePacket decode(ByteBuf buf) {
            var friendly = (net.minecraft.network.RegistryFriendlyByteBuf) buf;
            Component name = ComponentSerialization.STREAM_CODEC.decode(friendly);
            String socialClass = ByteBufCodecs.STRING_UTF8.decode(buf);
            boolean hasJob = ByteBufCodecs.BOOL.decode(buf);
            ResourceLocation jobId = hasJob ? ResourceLocation.STREAM_CODEC.decode(buf) : null;
            double happiness = buf.readDouble();
            UUID xoonglinId = UUIDUtil.STREAM_CODEC.decode(buf);
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            Map<String, NeedSatisfactionData> needsData = new HashMap<>();
            for (int i = 0; i < size; i++) {
                String needName = ByteBufCodecs.STRING_UTF8.decode(buf);
                double satisfaction = buf.readDouble();
                double damageThreshold = buf.readDouble();
                double satisfactionThreshold = buf.readDouble();
                needsData.put(needName, new NeedSatisfactionData(satisfaction, damageThreshold, satisfactionThreshold));
            }
            return new XoonglinInfoUpdatePacket(name, socialClass, jobId, happiness, needsData, xoonglinId);
        }

        @Override
        public void encode(ByteBuf buf, XoonglinInfoUpdatePacket packet) {
            var friendly = (net.minecraft.network.RegistryFriendlyByteBuf) buf;
            ComponentSerialization.STREAM_CODEC.encode(friendly, packet.name);
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.socialClass);
            ByteBufCodecs.BOOL.encode(buf, packet.jobId != null);
            if (packet.jobId != null) {
                ResourceLocation.STREAM_CODEC.encode(buf, packet.jobId);
            }
            buf.writeDouble(packet.happiness);
            UUIDUtil.STREAM_CODEC.encode(buf, packet.xoonglinId);
            ByteBufCodecs.VAR_INT.encode(buf, packet.needsData.size());
            for (Map.Entry<String, NeedSatisfactionData> entry : packet.needsData.entrySet()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, entry.getKey());
                buf.writeDouble(entry.getValue().satisfaction);
                buf.writeDouble(entry.getValue().damageThreshold);
                buf.writeDouble(entry.getValue().satisfactionThreshold);
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(XoonglinInfoUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof XoonglinInfoScreen screen) {
                screen.updateData(packet);
            }
        });
    }

    public Component getName() { return name; }
    public String getSocialClass() { return socialClass; }
    public ResourceLocation getJobId() { return jobId; }
    public double getHappiness() { return happiness; }
    public Map<String, NeedSatisfactionData> getNeedsData() { return needsData; }
    public UUID getXoonglinId() { return xoonglinId; }
}
