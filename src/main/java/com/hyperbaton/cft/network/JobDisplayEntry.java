package com.hyperbaton.cft.network;

import net.minecraft.resources.ResourceLocation;

public record JobDisplayEntry(byte type, String labelKey, String textValue, int intA, int intB, ResourceLocation icon) {
    public static final byte TEXT = 0;
    public static final byte PROGRESS = 1;
    public static final byte ITEM = 2;

    public static JobDisplayEntry text(String labelKey, String value, int color) {
        return new JobDisplayEntry(TEXT, labelKey, value, color, 0, null);
    }

    public static JobDisplayEntry progress(String labelKey, int current, int max) {
        return new JobDisplayEntry(PROGRESS, labelKey, null, current, max, null);
    }

    public static JobDisplayEntry progress(String labelKey, int current, int max, String displayText) {
        return new JobDisplayEntry(PROGRESS, labelKey, displayText, current, max, null);
    }

    public static JobDisplayEntry item(String labelKey, ResourceLocation icon, int count) {
        return new JobDisplayEntry(ITEM, labelKey, null, count, 0, icon);
    }
}
