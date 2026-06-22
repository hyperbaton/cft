package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.structure.home.XoonglinHome;
import com.hyperbaton.cft.world.HomesData;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record RequestHomeLabelsPacket() implements CustomPacketPayload {

    public static final Type<RequestHomeLabelsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "request_home_labels"));

    public static final StreamCodec<ByteBuf, RequestHomeLabelsPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RequestHomeLabelsPacket decode(ByteBuf buf) {
            return new RequestHomeLabelsPacket();
        }

        @Override
        public void encode(ByteBuf buf, RequestHomeLabelsPacket packet) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestHomeLabelsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                HomesData homesData = level.getDataStorage().computeIfAbsent(HomesData.factory(), "homesData");

                List<HomeLabelsPacket.Entry> entries = new ArrayList<>();
                for (XoonglinHome home : homesData.getHomes()) {
                    if (!home.getLeaderId().equals(player.getUUID())) continue;
                    if (home.getEntrance() == null) continue;

                    String label;
                    if (home.getOwnerId() != null) {
                        Entity owner = level.getEntity(home.getOwnerId());
                        if (owner instanceof XoonglinEntity xoonglin && xoonglin.getCustomName() != null) {
                            label = Component.translatable("gui.cft.home_label.occupied",
                                    xoonglin.getCustomName().getString()).getString();
                        } else {
                            label = Component.translatable(home.getSatisfiedNeed()).getString();
                        }
                    } else {
                        label = Component.translatable(home.getSatisfiedNeed()).getString();
                    }

                    Direction facing = Direction.NORTH;
                    BlockState state = level.getBlockState(home.getEntrance());
                    if (state.getBlock() instanceof DoorBlock) {
                        facing = state.getValue(DoorBlock.FACING);
                    }

                    entries.add(new HomeLabelsPacket.Entry(home.getEntrance(), label, facing));
                }

                PacketDistributor.sendToPlayer(player, new HomeLabelsPacket(entries));
            }
        });
    }
}
