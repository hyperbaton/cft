package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.TraderJob;
import com.hyperbaton.cft.job.TradeOffer;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Sent client -> server when a customer clicks a trade row on the TradeScreen. */
public record ExecuteTradePacket(UUID xoonglinId, int tradeIndex) implements CustomPacketPayload {

    public static final Type<ExecuteTradePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "execute_trade"));

    public static final StreamCodec<ByteBuf, ExecuteTradePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ExecuteTradePacket decode(ByteBuf buf) {
            UUID xoonglinId = UUIDUtil.STREAM_CODEC.decode(buf);
            int tradeIndex = ByteBufCodecs.VAR_INT.decode(buf);
            return new ExecuteTradePacket(xoonglinId, tradeIndex);
        }

        @Override
        public void encode(ByteBuf buf, ExecuteTradePacket packet) {
            UUIDUtil.STREAM_CODEC.encode(buf, packet.xoonglinId);
            ByteBufCodecs.VAR_INT.encode(buf, packet.tradeIndex);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ExecuteTradePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            if (!(level.getEntity(packet.xoonglinId) instanceof XoonglinEntity trader)) return;
            if (trader.getJob() == null || !(CftRegistry.JOBS.get(trader.getJob()) instanceof TraderJob)) return;
            if (trader.getLeaderId() != null && trader.getLeaderId().equals(player.getUUID())) return;

            var offers = trader.getTradeOffers();
            if (packet.tradeIndex < 0 || packet.tradeIndex >= offers.size()) return;
            TradeOffer offer = offers.get(packet.tradeIndex);
            if (!offer.isActive()) return;

            if (!hasEnough(trader.getInventory(), offer.given()) || !hasEnough(player.getInventory(), offer.wanted())) {
                return;
            }

            take(trader.getInventory(), offer.given());
            take(player.getInventory(), offer.wanted());

            ItemStack playerLeftover = offer.given().copy();
            player.getInventory().add(playerLeftover);
            if (!playerLeftover.isEmpty()) {
                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), playerLeftover);
            }
            ItemStack traderLeftover = trader.getInventory().addItem(offer.wanted().copy());
            if (!traderLeftover.isEmpty()) {
                Containers.dropItemStack(level, trader.getX(), trader.getY(), trader.getZ(), traderLeftover);
            }

            OpenTradeScreenPacket.sendTo(player, trader);
        });
    }

    private static boolean hasEnough(net.minecraft.world.Container container, ItemStack template) {
        int found = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
                found += stack.getCount();
                if (found >= template.getCount()) return true;
            }
        }
        return false;
    }

    private static void take(net.minecraft.world.Container container, ItemStack template) {
        int remaining = template.getCount();
        for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, template)) continue;
            int take = Math.min(remaining, stack.getCount());
            container.removeItem(i, take);
            remaining -= take;
        }
    }
}
