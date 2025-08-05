package com.hyperbaton.cft.network;

import com.hyperbaton.cft.network.client.HomeDetectionPacketClient;
import com.hyperbaton.cft.structure.home.HomeDetectionReasons;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

public class HomeDetectionPacket {
    private final boolean homeDetected;
    private final String homeNeed;
    private final HomeDetectionReasons detectionReason;
    private final List<String> validationDetails;

    public HomeDetectionPacket(boolean homeDetected, String homeNeed, HomeDetectionReasons errorReason, List<String> validationDetails) {
        this.homeDetected = homeDetected;
        this.homeNeed = homeNeed;
        this.detectionReason = errorReason;
        this.validationDetails = validationDetails;
    }

    public HomeDetectionPacket(FriendlyByteBuf buffer) {
        this.homeDetected = buffer.readBoolean();
        this.homeNeed = buffer.readUtf();
        this.detectionReason = HomeDetectionReasons.valueOf(buffer.readUtf());
        int size = buffer.readVarInt();
        this.validationDetails = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            this.validationDetails.add(buffer.readUtf());
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(homeDetected);
        buffer.writeUtf(homeNeed);
        buffer.writeUtf(detectionReason.name());
        buffer.writeVarInt(validationDetails.size());
        for (String detail : validationDetails) {
            buffer.writeUtf(detail);
        }
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

    public List<String> getValidationDetails() {
        return validationDetails;
    }
}