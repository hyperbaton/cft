package com.hyperbaton.cft.job;

import net.minecraft.nbt.CompoundTag;

public class JobState {
    public long lastDayIndex = Long.MIN_VALUE;
    public int workedTicksToday = 0;
    public int consecutiveDaysWorked = 0;
    /**
     * Game time of the last completed periodic action (e.g. an officiant's ritual).
     * Used by jobs whose work recurs on a frequency rather than a daily quota.
     */
    public long lastActionGameTime = 0;

    public void reset() {
        lastDayIndex = Long.MIN_VALUE;
        workedTicksToday = 0;
        consecutiveDaysWorked = 0;
        lastActionGameTime = 0;
    }

    /**
     * Clears only state tied to the specific job behavior (e.g. an officiant's last
     * ritual time). The daily work quota tracking is left untouched, since it reflects
     * the Xoonglin's workday and must survive job reassignment (otherwise switching
     * jobs would let a Xoonglin dodge its daily quota).
     */
    public void resetJobSpecific() {
        lastActionGameTime = 0;
    }

    public void save(CompoundTag tag) {
        tag.putLong("lastDay", lastDayIndex);
        tag.putInt("workedToday", workedTicksToday);
        tag.putInt("consecutive", consecutiveDaysWorked);
        tag.putLong("lastAction", lastActionGameTime);
    }

    public void load(CompoundTag tag) {
        lastDayIndex = tag.getLong("lastDay");
        workedTicksToday = tag.getInt("workedToday");
        consecutiveDaysWorked = tag.getInt("consecutive");
        lastActionGameTime = tag.getLong("lastAction");
    }
}
