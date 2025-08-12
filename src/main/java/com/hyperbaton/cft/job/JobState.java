package com.hyperbaton.cft.job;

import net.minecraft.nbt.CompoundTag;

public class JobState {
    public long lastDayIndex = Long.MIN_VALUE;
    public int workedTicksToday = 0;
    public int consecutiveDaysWorked = 0;

    public void save(CompoundTag tag) {
        tag.putLong("lastDay", lastDayIndex);
        tag.putInt("workedToday", workedTicksToday);
        tag.putInt("consecutive", consecutiveDaysWorked);
    }

    public void load(CompoundTag tag) {
        lastDayIndex = tag.getLong("lastDay");
        workedTicksToday = tag.getInt("workedToday");
        consecutiveDaysWorked = tag.getInt("consecutive");
    }
}
