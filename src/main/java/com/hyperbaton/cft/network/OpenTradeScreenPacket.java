package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.TradeOffer;
import com.hyperbaton.cft.network.client.OpenTradeScreenPacketClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Sent server -> client to open (or refresh) the customer-facing trade screen. */
public record OpenTradeScreenPacket(UUID xoonglinId, Component name, List<TradeEntryData> trades)
        implements CustomPacketPayload {

    public static final Type<OpenTradeScreenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "open_trade_screen"));

    public static final StreamCodec<ByteBuf, OpenTradeScreenPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenTradeScreenPacket decode(ByteBuf buf) {
            var friendly = (net.minecraft.network.RegistryFriendlyByteBuf) buf;
            UUID xoonglinId = UUIDUtil.STREAM_CODEC.decode(buf);
            Component name = ComponentSerialization.STREAM_CODEC.decode(friendly);
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            List<TradeEntryData> trades = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                ResourceLocation wantedItem = ResourceLocation.STREAM_CODEC.decode(buf);
                int wantedCount = ByteBufCodecs.VAR_INT.decode(buf);
                ResourceLocation givenItem = ResourceLocation.STREAM_CODEC.decode(buf);
                int givenCount = ByteBufCodecs.VAR_INT.decode(buf);
                int availableCount = ByteBufCodecs.VAR_INT.decode(buf);
                trades.add(new TradeEntryData(wantedItem, wantedCount, givenItem, givenCount, availableCount));
            }
            return new OpenTradeScreenPacket(xoonglinId, name, trades);
        }

        @Override
        public void encode(ByteBuf buf, OpenTradeScreenPacket packet) {
            var friendly = (net.minecraft.network.RegistryFriendlyByteBuf) buf;
            UUIDUtil.STREAM_CODEC.encode(buf, packet.xoonglinId);
            ComponentSerialization.STREAM_CODEC.encode(friendly, packet.name);
            ByteBufCodecs.VAR_INT.encode(buf, packet.trades.size());
            for (TradeEntryData trade : packet.trades) {
                ResourceLocation.STREAM_CODEC.encode(buf, trade.wantedItem());
                ByteBufCodecs.VAR_INT.encode(buf, trade.wantedCount());
                ResourceLocation.STREAM_CODEC.encode(buf, trade.givenItem());
                ByteBufCodecs.VAR_INT.encode(buf, trade.givenCount());
                ByteBufCodecs.VAR_INT.encode(buf, trade.availableCount());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenTradeScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> OpenTradeScreenPacketClient.handleClient(packet));
    }

    /** Builds and sends a fresh snapshot of the trader's active offers to a customer. */
    public static void sendTo(ServerPlayer player, XoonglinEntity trader) {
        List<TradeEntryData> trades = new ArrayList<>();
        for (TradeOffer offer : trader.getTradeOffers()) {
            if (!offer.isActive()) continue;
            int available = countInInventory(trader, offer.given());
            trades.add(new TradeEntryData(
                    BuiltInRegistries.ITEM.getKey(offer.wanted().getItem()), offer.wanted().getCount(),
                    BuiltInRegistries.ITEM.getKey(offer.given().getItem()), offer.given().getCount(),
                    available));
        }
        PacketDistributor.sendToPlayer(player,
                new OpenTradeScreenPacket(trader.getUUID(), trader.getCustomName() != null
                        ? trader.getCustomName() : trader.getName(), trades));
    }

    private static int countInInventory(XoonglinEntity trader, ItemStack template) {
        int found = 0;
        var inventory = trader.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
                found += stack.getCount();
            }
        }
        return found;
    }
}
