package com.hyperbaton.cft.network;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
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
        final Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(MutableComponent.create(this.name.getContents())
                    .withStyle(ChatFormatting.BOLD)
                    .append(Component.literal(" class: "))
                    .append(Component.translatable(this.socialClass)));
            player.sendSystemMessage(MutableComponent.create(this.name.getContents())
                    .append(Component.literal(" happiness: " + String.format("%.2f", this.happiness))));
            player.sendSystemMessage(MutableComponent.create(this.name.getContents())
                    .append(Component.literal(" needs: ")));
            for (Map.Entry<String, Double> entry : needsSatisfaction.entrySet()) {
                player.sendSystemMessage(Component.translatable(entry.getKey())
                        .append(" satisfied at " + String.format("%.2f", entry.getValue() * 100) + "%"));
            }
        }
        context.get().setPacketHandled(true);
    }
}
