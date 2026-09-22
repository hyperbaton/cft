package com.hyperbaton.cft.job;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * A single configured trade slot on a Trader xoonglin: give up `wanted`, receive `given`.
 * Runtime state configured by the leader at will (not datapack content), persisted
 * directly on the XoonglinEntity.
 */
public record TradeOffer(ItemStack wanted, ItemStack given) {

    public static final TradeOffer EMPTY = new TradeOffer(ItemStack.EMPTY, ItemStack.EMPTY);

    public boolean isActive() {
        return !wanted.isEmpty() && !given.isEmpty();
    }

    public CompoundTag toTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!wanted.isEmpty()) tag.put("wanted", wanted.save(registries));
        if (!given.isEmpty()) tag.put("given", given.save(registries));
        return tag;
    }

    public static TradeOffer fromTag(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack wanted = tag.contains("wanted", Tag.TAG_COMPOUND)
                ? ItemStack.parse(registries, tag.getCompound("wanted")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        ItemStack given = tag.contains("given", Tag.TAG_COMPOUND)
                ? ItemStack.parse(registries, tag.getCompound("given")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        return new TradeOffer(wanted, given);
    }
}
