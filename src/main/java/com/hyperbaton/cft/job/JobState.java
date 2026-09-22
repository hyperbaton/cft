package com.hyperbaton.cft.job;

import net.minecraft.nbt.CompoundTag;

public class JobState {
    public long lastDayIndex = Long.MIN_VALUE;
    public int workedTicksToday = 0;
    public int consecutiveDaysWorked = 0;
    /**
     * Whether today's quota has already been credited toward consecutiveDaysWorked (and,
     * for jobs with a multi-day streak, whether production has already been attempted for
     * it). Set the instant workedTicksToday first reaches the daily quota, rather than
     * waiting for the day to roll over. If a day ends with this still false, the quota
     * wasn't met and the streak resets to 0.
     */
    public boolean creditedToday = false;
    /**
     * Whether today's start-of-day action (e.g. a trader's restock) has already run.
     * Reset alongside creditedToday at day rollover. Distinct from creditedToday because
     * some jobs have separate start-of-day and end-of-day actions.
     */
    public boolean dayStartHandled = false;
    /**
     * Game time of the last completed periodic action (e.g. an officiant's ritual).
     * Used by jobs whose work recurs on a frequency rather than a daily quota.
     */
    public long lastActionGameTime = 0;

    public void reset() {
        lastDayIndex = Long.MIN_VALUE;
        workedTicksToday = 0;
        consecutiveDaysWorked = 0;
        creditedToday = false;
        dayStartHandled = false;
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
        tag.putBoolean("creditedToday", creditedToday);
        tag.putBoolean("dayStartHandled", dayStartHandled);
        tag.putLong("lastAction", lastActionGameTime);
    }

    public void load(CompoundTag tag) {
        lastDayIndex = tag.getLong("lastDay");
        workedTicksToday = tag.getInt("workedToday");
        consecutiveDaysWorked = tag.getInt("consecutive");
        creditedToday = tag.getBoolean("creditedToday");
        dayStartHandled = tag.getBoolean("dayStartHandled");
        lastActionGameTime = tag.getLong("lastAction");
    }
}
