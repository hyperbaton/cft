package com.hyperbaton.cft.network;

import com.hyperbaton.cft.network.client.CheckOnXoonglinPacketClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class CheckOnXoonglinPacket {
    private final Component name;
    private final String socialClass;
    private final double happiness;
    private final Map<String, NeedSatisfactionData> needsData;
    private final UUID xoonglinId;

    public CheckOnXoonglinPacket(Component name, String socialClass, double happiness, 
            Map<String, NeedSatisfactionData> needsData, UUID xoonglinId) {
        this.name = name;
        this.socialClass = socialClass;
        this.happiness = happiness;
        this.needsData = needsData;
        this.xoonglinId = xoonglinId;
    }

    public CheckOnXoonglinPacket(FriendlyByteBuf buffer) {
        this.name = buffer.readComponent();
        this.socialClass = buffer.readUtf();
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

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> CheckOnXoonglinPacketClient.handleClient(this));
        }
        ctx.setPacketHandled(true);
    }

    public Component getName() {
        return name;
    }

    public String getSocialClass() {
        return socialClass;
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