package com.hyperbaton.cft.network;

import com.hyperbaton.cft.client.gui.XoonglinInfoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class XoonglinInfoUpdatePacket {
    private final Component name;
    private final String socialClass;
    private final ResourceLocation jobId;
    private final double happiness;
    private final Map<String, NeedSatisfactionData> needsData;
    private final UUID xoonglinId;

    public XoonglinInfoUpdatePacket(Component name, String socialClass, ResourceLocation jobId, double happiness, 
            Map<String, NeedSatisfactionData> needsData, UUID xoonglinId) {
        this.name = name;
        this.socialClass = socialClass;
        this.jobId = jobId;
        this.happiness = happiness;
        this.needsData = needsData;
        this.xoonglinId = xoonglinId;
    }

    public XoonglinInfoUpdatePacket(FriendlyByteBuf buffer) {
        this.name = buffer.readComponent();
        this.socialClass = buffer.readUtf();
        this.jobId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        this.happiness = buffer.readDouble();
        this.xoonglinId = buffer.readUUID();
        this.needsData = new HashMap<>();
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            String needName = buffer.readUtf();
            double satisfaction = buffer.readDouble();
            double damageThreshold = buffer.readDouble();
            double satisfactionThreshold = buffer.readDouble();
            needsData.put(needName, new NeedSatisfactionData(satisfaction, damageThreshold, satisfactionThreshold));
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeComponent(name);
        buffer.writeUtf(socialClass);
        buffer.writeBoolean(jobId != null);
        if (jobId != null) {
            buffer.writeResourceLocation(jobId);
        }
        buffer.writeDouble(happiness);
        buffer.writeUUID(xoonglinId);
        buffer.writeVarInt(needsData.size());
        for (Map.Entry<String, NeedSatisfactionData> entry : needsData.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeDouble(entry.getValue().satisfaction);
            buffer.writeDouble(entry.getValue().damageThreshold);
            buffer.writeDouble(entry.getValue().satisfactionThreshold);
        }
    }

    public static void handle(XoonglinInfoUpdatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof XoonglinInfoScreen screen) {
                screen.updateData(packet);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public Component getName() {
        return name;
    }

    public String getSocialClass() {
        return socialClass;
    }

    public ResourceLocation getJobId() { // NEW
        return jobId;
    }

    public double getHappiness() {
        return happiness;
    }

    public Map<String, NeedSatisfactionData> getNeedsData() {
        return needsData;
    }

    public UUID getXoonglinId() {
        return xoonglinId;
    }
}