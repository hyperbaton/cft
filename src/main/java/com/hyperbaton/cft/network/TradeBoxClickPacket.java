package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.TradeOffer;
import com.hyperbaton.cft.menu.TradeConfigMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Sent client -> server when the leader clicks a trade box in TradeConfigScreen. Adds
 * the cursor's item (untouched, never consumed) into the box when the cursor is holding
 * something, or (with an empty cursor) just clears the box — since configuring a trade
 * never took the sampled item from the player, clearing it must not hand anything back.
 */
public record TradeBoxClickPacket(UUID xoonglinId, int tradeIndex, boolean wantedSide) implements CustomPacketPayload {

    public static final Type<TradeBoxClickPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "trade_box_click"));

    public static final StreamCodec<ByteBuf, TradeBoxClickPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TradeBoxClickPacket decode(ByteBuf buf) {
            UUID xoonglinId = UUIDUtil.STREAM_CODEC.decode(buf);
            int tradeIndex = ByteBufCodecs.VAR_INT.decode(buf);
            boolean wantedSide = buf.readBoolean();
            return new TradeBoxClickPacket(xoonglinId, tradeIndex, wantedSide);
        }

        @Override
        public void encode(ByteBuf buf, TradeBoxClickPacket packet) {
            UUIDUtil.STREAM_CODEC.encode(buf, packet.xoonglinId);
            ByteBufCodecs.VAR_INT.encode(buf, packet.tradeIndex);
            buf.writeBoolean(packet.wantedSide);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TradeBoxClickPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof TradeConfigMenu menu) || !menu.getXoonglinId().equals(packet.xoonglinId)) {
                return;
            }
            ServerLevel level = player.serverLevel();
            if (!(level.getEntity(packet.xoonglinId) instanceof XoonglinEntity xoonglin)) return;
            if (xoonglin.getLeaderId() == null || !xoonglin.getLeaderId().equals(player.getUUID())) return;
            if (packet.tradeIndex < 0 || packet.tradeIndex >= menu.getMaxTrades()) return;

            TradeOffer current = menu.getTradeOffersView().get(packet.tradeIndex);
            ItemStack currentBoxStack = packet.wantedSide ? current.wanted() : current.given();
            ItemStack cursor = menu.getCarried();

            TradeOffer updated;
            if (!cursor.isEmpty()) {
                if (currentBoxStack.isEmpty()) {
                    ItemStack newBox = cursor.copy();
                    updated = packet.wantedSide
                            ? new TradeOffer(newBox, current.given())
                            : new TradeOffer(current.wanted(), newBox);
                } else if (ItemStack.isSameItemSameComponents(currentBoxStack, cursor)) {
                    ItemStack grown = currentBoxStack.copy();
                    grown.grow(cursor.getCount());
                    updated = packet.wantedSide
                            ? new TradeOffer(grown, current.given())
                            : new TradeOffer(current.wanted(), grown);
                } else {
                    return; // different item type while occupied: must clear the box first
                }
            } else {
                if (currentBoxStack.isEmpty()) return;
                // Configuring a trade never consumes the sampled item (the cursor stack is
                // left untouched on add), so clearing a box must not give anything back either
                // — nothing was ever taken from the player's inventory in the first place.
                updated = packet.wantedSide
                        ? new TradeOffer(ItemStack.EMPTY, current.given())
                        : new TradeOffer(current.wanted(), ItemStack.EMPTY);
            }

            menu.setTradeOffer(packet.tradeIndex, updated);
            xoonglin.setTradeOffer(packet.tradeIndex, updated);
            PacketDistributor.sendToPlayer(player, new TradeBoxUpdatePacket(packet.tradeIndex, updated.wanted(), updated.given()));
        });
    }
}
