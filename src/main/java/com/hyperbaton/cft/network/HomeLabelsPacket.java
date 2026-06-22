package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.client.render.HomeLabelRenderer;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public record HomeLabelsPacket(List<Entry> entries) implements CustomPacketPayload {

    public record Entry(BlockPos pos, String label, Direction facing) {}

    public static final Type<HomeLabelsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "home_labels"));

    public static final StreamCodec<ByteBuf, HomeLabelsPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public HomeLabelsPacket decode(ByteBuf buf) {
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                String label = ByteBufCodecs.STRING_UTF8.decode(buf);
                Direction facing = Direction.from3DDataValue(buf.readByte());
                entries.add(new Entry(new BlockPos(x, y, z), label, facing));
            }
            return new HomeLabelsPacket(entries);
        }

        @Override
        public void encode(ByteBuf buf, HomeLabelsPacket packet) {
            ByteBufCodecs.VAR_INT.encode(buf, packet.entries.size());
            for (Entry entry : packet.entries) {
                buf.writeInt(entry.pos.getX());
                buf.writeInt(entry.pos.getY());
                buf.writeInt(entry.pos.getZ());
                ByteBufCodecs.STRING_UTF8.encode(buf, entry.label);
                buf.writeByte(entry.facing.get3DDataValue());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HomeLabelsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> HomeLabelRenderer.updateLabels(packet.entries));
    }
}
