package com.hyperbaton.cft;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-player presentation settings. They only affect what the local client shows, never
 * how Xoonglins behave, so each player can tune them without touching the server.
 */
public class CftClientConfig {

    public enum NeedIndicatorMode {
        /** Show both unsatisfied and critical needs at all times. */
        ALL,
        /** Show critical needs at all times; unsatisfied ones only while holding the leader staff. */
        CRITICAL,
        /** Show every alert, but only while holding the leader staff. */
        STAFF_ONLY,
        NONE
    }

    public enum NotificationMode {
        TOAST,
        CHAT,
        BOTH,
        NONE
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<NeedIndicatorMode> NEED_INDICATOR = BUILDER
            .comment("When to show the need icon floating above your Xoonglins.",
                    "ALL: always show unsatisfied and critical needs.",
                    "CRITICAL: always show critical (harmful) needs; unsatisfied ones only while holding the leader staff.",
                    "STAFF_ONLY: show every alert, but only while holding the leader staff.",
                    "NONE: never show it.")
            .defineEnum("needIndicator.mode", NeedIndicatorMode.CRITICAL);

    public static final ModConfigSpec.EnumValue<NotificationMode> CLASS_CHANGE_NOTIFICATIONS = BUILDER
            .comment("How to notify you when one of your Xoonglins changes social class.")
            .defineEnum("notifications.classChange", NotificationMode.TOAST);

    static final ModConfigSpec SPEC = BUILDER.build();
}
