package com.hyperbaton.cft.network;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.TradeOffer;
import com.hyperbaton.cft.job.TraderJob;
import com.hyperbaton.cft.menu.TradeConfigMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Sent client -> server when the leader clicks "Configure Trades" on the Job tab. */
public record RequestConfigureTradesPacket(UUID xoonglinId) implements CustomPacketPayload {

    public static final Type<RequestConfigureTradesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "request_configure_trades"));

    public static final StreamCodec<ByteBuf, RequestConfigureTradesPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RequestConfigureTradesPacket decode(ByteBuf buf) {
            return new RequestConfigureTradesPacket(UUIDUtil.STREAM_CODEC.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, RequestConfigureTradesPacket packet) {
            UUIDUtil.STREAM_CODEC.encode(buf, packet.xoonglinId);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestConfigureTradesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            if (!(level.getEntity(packet.xoonglinId) instanceof XoonglinEntity xoonglin)) return;
            if (xoonglin.getLeaderId() == null || !xoonglin.getLeaderId().equals(player.getUUID())) return;
            if (xoonglin.getJob() == null || !(CftRegistry.JOBS.get(xoonglin.getJob()) instanceof TraderJob traderJob)) return;

            int maxTrades = traderJob.getMaxTrades();
            List<TradeOffer> offers = new ArrayList<>(xoonglin.getTradeOffers());
            while (offers.size() < maxTrades) offers.add(TradeOffer.EMPTY);

            player.openMenu(new SimpleMenuProvider(
                    (windowId, inv, p) -> new TradeConfigMenu(windowId, inv, xoonglin.getUUID(), maxTrades, offers),
                    xoonglin.getCustomName() != null ? xoonglin.getCustomName() : xoonglin.getName()
            ), buf -> TradeConfigMenu.writeOpenData(buf, xoonglin.getUUID(), maxTrades, offers));
        });
    }
}
