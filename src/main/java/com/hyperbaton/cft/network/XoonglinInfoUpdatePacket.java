package com.hyperbaton.cft.network;

import com.hyperbaton.cft.client.gui.XoonglinInfoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class XoonglinInfoUpdatePacket {

    private final Component name;
    private final String socialClass;
    private final double happiness;
    private final Map<String, Double> needsSatisfaction;
    private final UUID xoonglinId;

    public XoonglinInfoUpdatePacket(Component name, String socialClass, double happiness, Map<String, Double> needsSatisfaction, UUID xoonglinId) {
        this.name = name;
        this.socialClass = socialClass;
        this.happiness = happiness;
        this.needsSatisfaction = needsSatisfaction;
        this.xoonglinId = xoonglinId;
    }

    public XoonglinInfoUpdatePacket(FriendlyByteBuf buffer) {
        this.name = buffer.readComponent();
        this.socialClass = buffer.readUtf();
        this.happiness = buffer.readDouble();
        this.xoonglinId = buffer.readUUID();
        this.needsSatisfaction = new HashMap<>();
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            needsSatisfaction.put(buffer.readUtf(), buffer.readDouble());
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeComponent(name);
        buffer.writeUtf(socialClass);
        buffer.writeDouble(happiness);
        buffer.writeUUID(xoonglinId);
        buffer.writeVarInt(needsSatisfaction.size());
        needsSatisfaction.forEach((key, value) -> {
            buffer.writeUtf(key);
            buffer.writeDouble(value);
        });
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

    public double getHappiness() {
        return happiness;
    }

    public Map<String, Double> getNeedsSatisfaction() {
        return needsSatisfaction;
    }

    public UUID getXoonglinId() {
        return xoonglinId;
    }
}
