package com.hyperbaton.cft.network;

import com.hyperbaton.cft.network.client.HomeDetectionPacketClient;
import com.hyperbaton.cft.structure.home.HomeDetectionReasons;
import net.minecraft.network.FriendlyByteBuf;
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
        NetworkEvent.Context ctx = context.get();
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> HomeDetectionPacketClient.handleClient(this));
        }
        ctx.setPacketHandled(true);
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
