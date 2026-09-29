package com.hyperbaton.cft.entity.ai.behavior;

/**
 * A step of a job behavior's work, shown in the job tab while the Xoonglin is working (e.g.
 * "Chopping a tree"). Behaviors use the standard steps below, which share their text, or define
 * their own with {@link #of(String)}.
 */
public record WorkStep(String key) {

    public static final WorkStep GOING_TO_WORK = of("going_to_work");
    public static final WorkStep FETCHING_SUPPLIES = of("fetching_supplies");
    public static final WorkStep STORING_ITEMS = of("storing_items");
    public static final WorkStep HEADING_BACK = of("heading_back");
    public static final WorkStep WAITING = of("waiting");

    /** A step whose text is the lang entry {@code gui.cft.work_step.<name>}. */
    public static WorkStep of(String name) {
        return new WorkStep("gui.cft.work_step." + name);
    }
}
