package com.hyperbaton.cft.network;

import net.minecraft.resources.ResourceLocation;

/** A snapshot of one trade offer for the customer-facing trade screen. */
public record TradeEntryData(ResourceLocation wantedItem, int wantedCount,
                              ResourceLocation givenItem, int givenCount,
                              int availableCount) {
}
