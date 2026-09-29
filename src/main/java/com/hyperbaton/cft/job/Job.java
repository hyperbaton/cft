package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.entity.ai.schedule.ScheduleDefinition;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Optional;

public abstract class Job {

    public static final Codec<Job> JOB_CODEC = Codec.lazyInitialized(() -> CftRegistry.JOBS_CODEC_REGISTRY.byNameCodec()
            .dispatch("type", Job::jobType, codec -> withCommonFields(codec)));

    /**
     * Adds the fields shared by every job type to its codec, so each job codec doesn't have
     * to declare them (some are already at the codec builder's field limit).
     */
    private static <J extends Job> MapCodec<J> withCommonFields(Codec<J> codec) {
        return Codec.mapPair(MapCodec.assumeMapUnsafe(codec), ScheduleDefinition.CODEC.optionalFieldOf("schedule"))
                .xmap(pair -> {
                    J job = pair.getFirst();
                    ((Job) job).schedule = pair.getSecond().orElse(null);
                    return job;
                }, job -> Pair.of(job, job.getSchedule()));
    }

    private final List<String> requiredNeeds;
    /** Replaces the social class schedule for Xoonglins with this job; null to use the class one. */
    private ScheduleDefinition schedule;
    private final double minHappiness;
    private final boolean availableToBabies;
    private final boolean availableToAdults;

    protected Job(List<String> requiredNeeds) {
        this(requiredNeeds, 0.0);
    }

    protected Job(List<String> requiredNeeds, double minHappiness) {
        this(requiredNeeds, minHappiness, false, true);
    }

    protected Job(List<String> requiredNeeds, double minHappiness, boolean availableToBabies, boolean availableToAdults) {
        this.requiredNeeds = requiredNeeds != null ? List.copyOf(requiredNeeds) : List.of();
        this.minHappiness = minHappiness;
        this.availableToBabies = availableToBabies;
        this.availableToAdults = availableToAdults;
    }

    public abstract void tick(XoonglinEntity xoonglin, JobState state);

    /**
     * Erases the brain memories this job sets to trigger its behaviors. Called instead
     * of tick() while the job is preempted (e.g. the xoonglin was summoned to attend a
     * ritual), so the job's behaviors stop cleanly until the job resumes ticking.
     */
    public abstract void eraseMemories(XoonglinEntity xoonglin);

    public abstract JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state);

    String idHint() { return getClass().getSimpleName(); }

    public abstract Codec<? extends Job> jobType();

    public List<String> getRequiredNeeds() {
        return requiredNeeds;
    }

    public double getMinHappiness() {
        return minHappiness;
    }

    public boolean isAvailableToBabies() {
        return availableToBabies;
    }

    public boolean isAvailableToAdults() {
        return availableToAdults;
    }

    public boolean canWork(XoonglinEntity xoonglin) {
        if (!xoonglin.allDamagingNeedsSatisfied()) return false;

        if (xoonglin.getHappiness() < minHappiness) return false;

        if (!requiredNeeds.isEmpty() && xoonglin.getNeeds() != null) {
            for (String needId : requiredNeeds) {
                boolean satisfied = xoonglin.getNeeds().stream()
                        .filter(ns -> ns.getNeed().getId().equals(needId))
                        .anyMatch(NeedSatisfier::isSatisfied);
                if (!satisfied) return false;
            }
        }

        return true;
    }

    public Optional<ScheduleDefinition> getSchedule() {
        return Optional.ofNullable(schedule);
    }

    public String getRequiredStructureType() {
        return null;
    }

}
