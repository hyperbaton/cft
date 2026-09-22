package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.network.client.TradeBoxUpdatePacketClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sent server -> client after a TradeBoxClickPacket, to sync the changed box back. */
public record TradeBoxUpdatePacket(int tradeIndex, ItemStack wanted, ItemStack given) implements CustomPacketPayload {

    public static final Type<TradeBoxUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "trade_box_update"));

    public static final StreamCodec<ByteBuf, TradeBoxUpdatePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TradeBoxUpdatePacket decode(ByteBuf buf) {
            int tradeIndex = ByteBufCodecs.VAR_INT.decode(buf);
            ItemStack wanted = readStack(buf);
            ItemStack given = readStack(buf);
            return new TradeBoxUpdatePacket(tradeIndex, wanted, given);
        }

        @Override
        public void encode(ByteBuf buf, TradeBoxUpdatePacket packet) {
            ByteBufCodecs.VAR_INT.encode(buf, packet.tradeIndex);
            writeStack(buf, packet.wanted);
            writeStack(buf, packet.given);
        }

        private void writeStack(ByteBuf buf, ItemStack stack) {
            buf.writeBoolean(!stack.isEmpty());
            if (!stack.isEmpty()) {
                ResourceLocation.STREAM_CODEC.encode(buf, BuiltInRegistries.ITEM.getKey(stack.getItem()));
                ByteBufCodecs.VAR_INT.encode(buf, stack.getCount());
            }
        }

        private ItemStack readStack(ByteBuf buf) {
            boolean present = buf.readBoolean();
            if (!present) return ItemStack.EMPTY;
            ResourceLocation item = ResourceLocation.STREAM_CODEC.decode(buf);
            int count = ByteBufCodecs.VAR_INT.decode(buf);
            return new ItemStack(BuiltInRegistries.ITEM.get(item), count);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TradeBoxUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> TradeBoxUpdatePacketClient.handleClient(packet));
    }
}
