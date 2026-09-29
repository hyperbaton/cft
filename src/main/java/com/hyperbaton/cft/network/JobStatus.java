package com.hyperbaton.cft.network;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * What a Xoonglin is doing at its job, as shown in the job tab: a translation key and a color that
 * tells at a glance how things are going, plus an optional detail line (e.g. why it can't work, or
 * the step of its work it's on). Jobs use the shared statuses below, or define their own with the
 * factory for the kind of status it is.
 */
public record JobStatus(String key, int color, @Nullable Component detail) {

    public JobStatus(String key, int color) {
        this(key, color, null);
    }

    /** Something keeps it from working, or needs the player's attention. */
    public static final int ATTENTION_COLOR = 0xDD4040;
    /** It's doing its job. */
    public static final int ACTIVE_COLOR = 0x40AA40;
    /** It's done its work for the day. */
    public static final int DONE_COLOR = 0xDDAA00;
    /** It's on its way to work. */
    public static final int MOVING_COLOR = 0x4080DD;
    public static final int SLEEPING_COLOR = 0x6060C0;
    /** Outside its working hours. */
    public static final int OFF_DUTY_COLOR = 0x808080;

    public static final JobStatus NO_STRUCTURE = attention("gui.cft.job_status.no_structure");
    public static final JobStatus CANT_WORK = attention("gui.cft.job_status.cant_work");
    public static final JobStatus WORKING = active("gui.cft.job_status.working");
    public static final JobStatus RESTING = new JobStatus("gui.cft.job_status.resting", DONE_COLOR);
    public static final JobStatus TRAVELING = new JobStatus("gui.cft.job_status.traveling", MOVING_COLOR);
    public static final JobStatus PAUSED = new JobStatus("gui.cft.job_status.paused", MOVING_COLOR);

    // Shown instead of the job's status outside working time (see JobUtil#buildJobInfo)
    public static final JobStatus SLEEPING = new JobStatus("gui.cft.job_status.sleeping", SLEEPING_COLOR);
    public static final JobStatus AT_HOME = offDuty("gui.cft.job_status.at_home");
    public static final JobStatus FREE_TIME = offDuty("gui.cft.job_status.free_time");
    public static final JobStatus VISITING = offDuty("gui.cft.job_status.visiting");
    public static final JobStatus CHATTING = offDuty("gui.cft.job_status.chatting");

    /** The same status with a detail line under it; null for none. */
    public JobStatus withDetail(@Nullable Component detail) {
        return new JobStatus(key, color, detail);
    }

    /** A status for something that keeps it from working or needs the player's attention. */
    public static JobStatus attention(String key) {
        return new JobStatus(key, ATTENTION_COLOR);
    }

    /** A status for doing its job in some particular way. */
    public static JobStatus active(String key) {
        return new JobStatus(key, ACTIVE_COLOR);
    }

    private static JobStatus offDuty(String key) {
        return new JobStatus(key, OFF_DUTY_COLOR);
    }
}
