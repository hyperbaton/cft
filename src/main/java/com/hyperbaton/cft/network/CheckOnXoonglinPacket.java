package com.hyperbaton.cft.network;

import com.hyperbaton.cft.network.client.CheckOnXoonglinPacketClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CheckOnXoonglinPacket {

    private final Component name;
    private final String socialClass;
    private final double happiness;
    private final Map<String, Double> needsSatisfaction;

    public CheckOnXoonglinPacket(Component name, String socialClass, double happiness, Map<String, Double> needsSatisfaction) {
        this.name = name;
        this.socialClass = socialClass;
        this.happiness = happiness;
        this.needsSatisfaction = needsSatisfaction;
    }

    public CheckOnXoonglinPacket(FriendlyByteBuf buffer) {
        this.name = buffer.readComponent();
        this.socialClass = buffer.readUtf();
        this.happiness = buffer.readDouble();
        this.needsSatisfaction = new HashMap<>();
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            String needName = buffer.readUtf();
            double needSatisfaction = buffer.readDouble();
            needsSatisfaction.put(needName, needSatisfaction);
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeComponent(name);
        buffer.writeUtf(socialClass);
        buffer.writeDouble(happiness);
        buffer.writeVarInt(needsSatisfaction.size());
        for (Map.Entry<String, Double> entry : needsSatisfaction.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeDouble(entry.getValue());
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

    public Map<String, Double> getNeedsSatisfaction() {
        return needsSatisfaction;
    }
}
