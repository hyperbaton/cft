package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.network.client.CheckOnXoonglinPacketClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CheckOnXoonglinPacket(
        Component name,
        String socialClass,
        ResourceLocation jobId,
        double happiness,
        Map<String, NeedSatisfactionData> needsData,
        UUID xoonglinId,
        JobInfoData jobInfo,
        List<InventorySlotData> inventoryData,
        List<ResourceLocation> availableJobs,
        boolean canMate
) implements CustomPacketPayload {

    public static final Type<CheckOnXoonglinPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "check_on_xoonglin"));

    public static final StreamCodec<ByteBuf, CheckOnXoonglinPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CheckOnXoonglinPacket decode(ByteBuf buf) {
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
                int iconCount = ByteBufCodecs.VAR_INT.decode(buf);
                List<ResourceLocation> icons = new ArrayList<>();
                for (int j = 0; j < iconCount; j++) {
                    icons.add(ResourceLocation.STREAM_CODEC.decode(buf));
                }
                needsData.put(needName, new NeedSatisfactionData(satisfaction, damageThreshold, satisfactionThreshold, icons));
            }
            boolean hasJobInfo = ByteBufCodecs.BOOL.decode(buf);
            JobInfoData jobInfo = hasJobInfo ? JobInfoData.decode(buf) : null;
            int invSize = ByteBufCodecs.VAR_INT.decode(buf);
            List<InventorySlotData> inventoryData = new ArrayList<>();
            for (int i = 0; i < invSize; i++) {
                ResourceLocation item = ResourceLocation.STREAM_CODEC.decode(buf);
                int count = ByteBufCodecs.VAR_INT.decode(buf);
                inventoryData.add(new InventorySlotData(item, count));
            }
            int jobsSize = ByteBufCodecs.VAR_INT.decode(buf);
            List<ResourceLocation> availableJobs = new ArrayList<>();
            for (int i = 0; i < jobsSize; i++) {
                availableJobs.add(ResourceLocation.STREAM_CODEC.decode(buf));
            }
            boolean canMate = ByteBufCodecs.BOOL.decode(buf);
            return new CheckOnXoonglinPacket(name, socialClass, jobId, happiness, needsData, xoonglinId, jobInfo, inventoryData, availableJobs, canMate);
        }

        @Override
        public void encode(ByteBuf buf, CheckOnXoonglinPacket packet) {
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
                ByteBufCodecs.VAR_INT.encode(buf, entry.getValue().icons.size());
                for (ResourceLocation icon : entry.getValue().icons) {
                    ResourceLocation.STREAM_CODEC.encode(buf, icon);
                }
            }
            ByteBufCodecs.BOOL.encode(buf, packet.jobInfo != null);
            if (packet.jobInfo != null) {
                JobInfoData.encode(buf, packet.jobInfo);
            }
            ByteBufCodecs.VAR_INT.encode(buf, packet.inventoryData.size());
            for (InventorySlotData slot : packet.inventoryData) {
                ResourceLocation.STREAM_CODEC.encode(buf, slot.item());
                ByteBufCodecs.VAR_INT.encode(buf, slot.count());
            }
            ByteBufCodecs.VAR_INT.encode(buf, packet.availableJobs.size());
            for (ResourceLocation jobId : packet.availableJobs) {
                ResourceLocation.STREAM_CODEC.encode(buf, jobId);
            }
            ByteBufCodecs.BOOL.encode(buf, packet.canMate);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CheckOnXoonglinPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> CheckOnXoonglinPacketClient.handleClient(packet));
    }

    public Component getName() { return name; }
    public String getSocialClass() { return socialClass; }
    public ResourceLocation getJobId() { return jobId; }
    public double getHappiness() { return happiness; }
    public Map<String, NeedSatisfactionData> getNeedsData() { return needsData; }
    public UUID getXoonglinId() { return xoonglinId; }
    public JobInfoData getJobInfo() { return jobInfo; }
    public List<InventorySlotData> getInventoryData() { return inventoryData; }
    public List<ResourceLocation> getAvailableJobs() { return availableJobs; }
    public boolean isCanMate() { return canMate; }
}
