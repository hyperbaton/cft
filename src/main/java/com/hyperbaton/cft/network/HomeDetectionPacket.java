package com.hyperbaton.cft.network;

import com.hyperbaton.cft.structure.home.HomeDetectionReasons;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HomeDetectionPacket {

    private final boolean homeDetected;
    private final String homeNeed;
    private final HomeDetectionReasons detectionReason;

    public HomeDetectionPacket(boolean homeDetected, String homeNeed, HomeDetectionReasons errorReason) {
        this.homeDetected = homeDetected;
        this.homeNeed = homeNeed;
        this.detectionReason = errorReason;
    }

    public HomeDetectionPacket(FriendlyByteBuf buffer) {
        this.homeDetected = buffer.readBoolean();
        this.homeNeed = buffer.readUtf();
        this.detectionReason = HomeDetectionReasons.valueOf(buffer.readUtf());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(homeDetected);
        buffer.writeUtf(homeNeed);
        buffer.writeUtf(detectionReason.name());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        final Player player = Minecraft.getInstance().player;
        if (player != null) {
            if(!this.homeNeed.isBlank()){
                player.sendSystemMessage(Component.literal("Inspecting house of type ")
                        .append(Component.translatable(this.homeNeed)));
            } else {
                player.sendSystemMessage(Component.literal("Looking for a home..."));
            }
            player.sendSystemMessage(Component.literal(this.detectionReason.getMessage()));
        }
        context.get().setPacketHandled(true);
    }

    public boolean isHomeDetected() {
        return homeDetected;
    }

    public String getHomeNeed() {
        return homeNeed;
    }

    public HomeDetectionReasons getDetectionReason() {
        return detectionReason;
    }
}
