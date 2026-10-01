package com.hyperbaton.cft.util;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.schedule.ScheduleDefinition;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.Job;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Picks the schedule a Xoonglin follows and answers what it says right now.
 * <p>
 * A Xoonglin without a schedule keeps vanilla's {@link Schedule#EMPTY} on its brain and
 * behaves as default: working from sunrise until its daily hours are done.
 */
public final class ScheduleUtil {
    private ScheduleUtil() {}

    /**
     * Puts on the brain the schedule of the Xoonglin's job or, if the job has none, the one
     * of its social class. Called every AI step, since the job or class may have changed.
     */
    public static void updateBrainSchedule(XoonglinEntity xoonglin) {
        Schedule schedule = definitionFor(xoonglin)
                .map(ScheduleDefinition::getSchedule)
                .orElse(Schedule.EMPTY);
        if (xoonglin.getBrain().getSchedule() != schedule) {
            xoonglin.getBrain().setSchedule(schedule);
        }
    }

    /** Whether the Xoonglin follows a schedule here; dimensions without a day cycle ignore them. */
    public static boolean hasSchedule(XoonglinEntity xoonglin) {
        return xoonglin.getBrain().getSchedule() != Schedule.EMPTY
                && !xoonglin.level().dimensionType().hasFixedTime();
    }

    /** The activity the schedule calls for right now, or empty if the Xoonglin has no schedule. */
    public static Optional<Activity> currentActivity(XoonglinEntity xoonglin) {
        if (!hasSchedule(xoonglin)) return Optional.empty();
        int dayTime = (int) Math.floorMod(xoonglin.level().getDayTime(), 24000L);
        return Optional.of(xoonglin.getBrain().getSchedule().getActivityAt(dayTime));
    }

    public static boolean is(XoonglinEntity xoonglin, Activity activity) {
        return currentActivity(xoonglin).map(current -> current == activity).orElse(false);
    }

    /** Whether the Xoonglin has a schedule and it's not working time. */
    public static boolean isOffDuty(XoonglinEntity xoonglin) {
        return currentActivity(xoonglin).map(current -> current != Activity.WORK).orElse(false);
    }

    /**
     * Whether the Xoonglin may sleep now: during the rest time of its schedule or, if it has
     * no schedule, at night.
     */
    public static boolean isSleepTime(XoonglinEntity xoonglin) {
        Level level = xoonglin.level();
        return currentActivity(xoonglin)
                .map(current -> current == Activity.REST)
                .orElseGet(level::isNight);
    }

    private static Optional<ScheduleDefinition> definitionFor(XoonglinEntity xoonglin) {
        if (xoonglin.getJob() != null && CftRegistry.JOBS != null) {
            Job job = CftRegistry.JOBS.get(xoonglin.getJob());
            Optional<ScheduleDefinition> jobSchedule = job != null ? job.getSchedule() : Optional.empty();
            if (jobSchedule.filter(ScheduleUtil::isDefined).isPresent()) {
                return jobSchedule;
            }
        }
        return Optional.ofNullable(xoonglin.getSocialClass())
                .flatMap(socialClass -> socialClass.getSchedule())
                .filter(ScheduleUtil::isDefined);
    }

    /** An empty list of transitions counts as no schedule, rather than as idling all day. */
    private static boolean isDefined(ScheduleDefinition definition) {
        return !definition.getTransitions().isEmpty();
    }
}
