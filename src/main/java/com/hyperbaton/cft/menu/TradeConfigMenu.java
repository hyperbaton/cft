package com.hyperbaton.cft.menu;

import com.hyperbaton.cft.job.TradeOffer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Owner-facing trade configuration menu: real vanilla Slots for the 36 player-inventory
 * slots, plus a plain (non-Slot) in-memory list of trade boxes rendered and edited by
 * TradeConfigScreen via a dedicated TradeBoxClickPacket rather than vanilla's clicked().
 */
public class TradeConfigMenu extends AbstractContainerMenu {

    public static final int TRADE_ROW_HEIGHT = 20;
    public static final int TRADES_AREA_TOP = 18;
    /** How many trade rows are shown at once before the list becomes scrollable. */
    public static final int MAX_VISIBLE_TRADES = 5;

    private final UUID xoonglinId;
    private final int maxTrades;
    private final List<TradeOffer> tradeOffers;
    private final int playerInvY;

    public TradeConfigMenu(int windowId, Inventory playerInv, UUID xoonglinId, int maxTrades, List<TradeOffer> tradeOffers) {
        super(com.hyperbaton.cft.menu.CftMenus.TRADE_CONFIG.get(), windowId);
        this.xoonglinId = xoonglinId;
        this.maxTrades = Math.max(1, maxTrades);
        this.tradeOffers = new ArrayList<>(tradeOffers);
        while (this.tradeOffers.size() < this.maxTrades) {
            this.tradeOffers.add(TradeOffer.EMPTY);
        }

        int visibleRows = Math.min(this.maxTrades, MAX_VISIBLE_TRADES);
        this.playerInvY = TRADES_AREA_TOP + visibleRows * TRADE_ROW_HEIGHT + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        int hotbarY = playerInvY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY));
        }
    }

    public TradeConfigMenu(int windowId, Inventory playerInv, RegistryFriendlyByteBuf data) {
        this(windowId, playerInv, data.readUUID(), data.readVarInt(), readOffers(data));
    }

    public static void writeOpenData(RegistryFriendlyByteBuf buf, UUID xoonglinId, int maxTrades, List<TradeOffer> offers) {
        buf.writeUUID(xoonglinId);
        buf.writeVarInt(maxTrades);
        buf.writeVarInt(offers.size());
        for (TradeOffer offer : offers) {
            writeStack(buf, offer.wanted());
            writeStack(buf, offer.given());
        }
    }

    private static List<TradeOffer> readOffers(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<TradeOffer> offers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            offers.add(new TradeOffer(readStack(buf), readStack(buf)));
        }
        return offers;
    }

    private static void writeStack(RegistryFriendlyByteBuf buf, ItemStack stack) {
        buf.writeBoolean(!stack.isEmpty());
        if (!stack.isEmpty()) {
            ResourceLocation.STREAM_CODEC.encode(buf, BuiltInRegistries.ITEM.getKey(stack.getItem()));
            buf.writeVarInt(stack.getCount());
        }
    }

    private static ItemStack readStack(RegistryFriendlyByteBuf buf) {
        boolean present = buf.readBoolean();
        if (!present) return ItemStack.EMPTY;
        ResourceLocation item = ResourceLocation.STREAM_CODEC.decode(buf);
        int count = buf.readVarInt();
        return new ItemStack(BuiltInRegistries.ITEM.get(item), count);
    }

    public UUID getXoonglinId() {
        return xoonglinId;
    }

    public int getMaxTrades() {
        return maxTrades;
    }

    public int getPlayerInvY() {
        return playerInvY;
    }

    public List<TradeOffer> getTradeOffersView() {
        return tradeOffers;
    }

    public void setTradeOffer(int index, TradeOffer offer) {
        if (index >= 0 && index < tradeOffers.size()) {
            tradeOffers.set(index, offer);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int mainStart = 0, mainEnd = 27, hotbarEnd = 36;
            if (index < mainEnd) {
                if (!this.moveItemStackTo(stack, mainEnd, hotbarEnd, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, mainStart, mainEnd, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
