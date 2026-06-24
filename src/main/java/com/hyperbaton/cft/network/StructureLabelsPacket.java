package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.client.render.StructureLabelRenderer;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record StructureLabelsPacket(List<Entry> entries) implements CustomPacketPayload {

    public record Entry(BlockPos pos, String label) {}

    public static final Type<StructureLabelsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "structure_labels"));

    public static final StreamCodec<ByteBuf, StructureLabelsPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public StructureLabelsPacket decode(ByteBuf buf) {
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                String label = ByteBufCodecs.STRING_UTF8.decode(buf);
                entries.add(new Entry(new BlockPos(x, y, z), label));
            }
            return new StructureLabelsPacket(entries);
        }

        @Override
        public void encode(ByteBuf buf, StructureLabelsPacket packet) {
            ByteBufCodecs.VAR_INT.encode(buf, packet.entries.size());
            for (Entry entry : packet.entries) {
                buf.writeInt(entry.pos.getX());
                buf.writeInt(entry.pos.getY());
                buf.writeInt(entry.pos.getZ());
                ByteBufCodecs.STRING_UTF8.encode(buf, entry.label);
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StructureLabelsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> StructureLabelRenderer.updateLabels(packet.entries));
    }
}
