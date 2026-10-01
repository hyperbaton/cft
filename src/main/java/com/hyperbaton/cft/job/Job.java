package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.network.JobStatus;
import com.hyperbaton.cft.entity.ai.schedule.ScheduleDefinition;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

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

    /**
     * The memory this job sets while the Xoonglin has work to do. It triggers the job's behavior in
     * the WORK activity and tells whether the Xoonglin is working.
     */
    public abstract MemoryModuleType<Boolean> getWorkMemory();

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
        return cantWorkReason(xoonglin).isEmpty();
    }

    /**
     * Why the Xoonglin can't do this job right now, to show the player what to fix: an unsatisfied
     * need that damages it, too little happiness, or an unsatisfied need the job requires. Empty
     * if it can work.
     */
    public Optional<Component> cantWorkReason(XoonglinEntity xoonglin) {
        if (xoonglin.getNeeds() == null) return Optional.of(Component.empty());

        Optional<String> damagingNeed = xoonglin.getNeeds().stream()
                .filter(ns -> ns.getNeed().getDamage() > 0.0 && !ns.isSatisfied())
                .map(NeedSatisfier::getNeedId)
                .findFirst();
        if (damagingNeed.isPresent()) {
            return Optional.of(unsatisfiedNeedReason(damagingNeed.get()));
        }

        if (xoonglin.getHappiness() < minHappiness) {
            return Optional.of(Component.translatable("gui.cft.job_detail.too_unhappy",
                    String.format("%.2f", minHappiness)));
        }

        for (String needId : requiredNeeds) {
            boolean satisfied = xoonglin.getNeeds().stream()
                    .filter(ns -> needId.equals(ns.getNeedId()))
                    .anyMatch(NeedSatisfier::isSatisfied);
            if (!satisfied) return Optional.of(unsatisfiedNeedReason(needId));
        }

        return Optional.empty();
    }

    private static Component unsatisfiedNeedReason(String needId) {
        return Component.translatable("gui.cft.job_detail.unsatisfied_need", Component.translatable(needId));
    }

    /** {@link JobStatus#CANT_WORK}, saying why. */
    protected JobStatus cantWorkStatus(XoonglinEntity xoonglin) {
        return JobStatus.CANT_WORK.withDetail(cantWorkReason(xoonglin).orElse(null));
    }

    /** {@link JobStatus#NO_STRUCTURE}, naming the structure it needs. */
    protected JobStatus noStructureStatus() {
        String structureType = getRequiredStructureType();
        return structureType == null ? JobStatus.NO_STRUCTURE
                : JobStatus.NO_STRUCTURE.withDetail(Component.translatable("gui.cft.job_detail.needs_structure",
                        Component.translatable(structureType)));
    }

    public Optional<ScheduleDefinition> getSchedule() {
        return Optional.ofNullable(schedule);
    }

    public String getRequiredStructureType() {
        return null;
    }

    /**
     * Whether the work takes its structure apart (e.g. a quarry being dug out), so the structure stops
     * passing detection while in use. Then the structure isn't detected again while the Xoonglin
     * works it, and the job retires it itself.
     */
    public boolean consumesStructure() {
        return false;
    }

}
