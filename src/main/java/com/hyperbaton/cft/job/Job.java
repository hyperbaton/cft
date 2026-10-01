package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.network.JobStatus;
import com.hyperbaton.cft.entity.ai.schedule.ScheduleDefinition;
import com.hyperbaton.cft.util.LangUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.List;
import java.util.Optional;

public abstract class Job {

    public static final Codec<Job> JOB_CODEC = Codec.lazyInitialized(() -> CftRegistry.JOBS_CODEC_REGISTRY.byNameCodec()
            .dispatch("type", Job::jobType, codec -> MapCodec.assumeMapUnsafe(codec)));

    /**
     * The fields every job has, read along with the job's own fields (they're in the same JSON
     * object). A job type's codec includes them with {@link #propertiesCodec()} and passes them to
     * the {@link Job} constructor, as vanilla blocks do with their properties.
     *
     * @param requiredNeeds     needs that must be satisfied for the Xoonglin to work
     * @param minHappiness      the happiness the Xoonglin needs to work
     * @param schedule          replaces the social class schedule for Xoonglins with this job
     */
    public record Properties(List<ResourceLocation> requiredNeeds, double minHappiness, boolean availableToBabies,
                             boolean availableToAdults, Optional<ScheduleDefinition> schedule) {
        public static final MapCodec<Properties> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Properties::requiredNeeds),
                Codec.DOUBLE.optionalFieldOf("min_happiness", 0.0).forGetter(Properties::minHappiness),
                Codec.BOOL.optionalFieldOf("available_to_babies", false).forGetter(Properties::availableToBabies),
                Codec.BOOL.optionalFieldOf("available_to_adults", true).forGetter(Properties::availableToAdults),
                ScheduleDefinition.CODEC.optionalFieldOf("schedule").forGetter(Properties::schedule)
        ).apply(instance, Properties::new));
    }

    /** The fields every job has, for a job type's codec; it takes a single field of the codec. */
    protected static <J extends Job> RecordCodecBuilder<J, Properties> propertiesCodec() {
        return Properties.MAP_CODEC.forGetter(Job::getProperties);
    }

    private final Properties properties;

    protected Job(Properties properties) {
        this.properties = properties;
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

    public abstract Codec<? extends Job> jobType();

    public Properties getProperties() {
        return properties;
    }

    public List<ResourceLocation> getRequiredNeeds() {
        return properties.requiredNeeds();
    }

    public double getMinHappiness() {
        return properties.minHappiness();
    }

    public boolean isAvailableToBabies() {
        return properties.availableToBabies();
    }

    public boolean isAvailableToAdults() {
        return properties.availableToAdults();
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

        Optional<ResourceLocation> damagingNeed = xoonglin.getNeeds().stream()
                .filter(ns -> ns.getNeed().getDamage() > 0.0 && !ns.isSatisfied())
                .map(NeedSatisfier::getNeedId)
                .findFirst();
        if (damagingNeed.isPresent()) {
            return Optional.of(unsatisfiedNeedReason(damagingNeed.get()));
        }

        if (xoonglin.getHappiness() < getMinHappiness()) {
            return Optional.of(Component.translatable("gui.cft.job_detail.too_unhappy",
                    String.format("%.2f", getMinHappiness())));
        }

        for (ResourceLocation needId : getRequiredNeeds()) {
            boolean satisfied = xoonglin.getNeeds().stream()
                    .filter(ns -> needId.equals(ns.getNeedId()))
                    .anyMatch(NeedSatisfier::isSatisfied);
            if (!satisfied) return Optional.of(unsatisfiedNeedReason(needId));
        }

        return Optional.empty();
    }

    private static Component unsatisfiedNeedReason(ResourceLocation needId) {
        return Component.translatable("gui.cft.job_detail.unsatisfied_need", LangUtil.needName(needId));
    }

    /** {@link JobStatus#CANT_WORK}, saying why. */
    protected JobStatus cantWorkStatus(XoonglinEntity xoonglin) {
        return JobStatus.CANT_WORK.withDetail(cantWorkReason(xoonglin).orElse(null));
    }

    /** {@link JobStatus#NO_STRUCTURE}, naming the structure it needs. */
    protected JobStatus noStructureStatus() {
        ResourceLocation structureType = getRequiredStructureType();
        return structureType == null ? JobStatus.NO_STRUCTURE
                : JobStatus.NO_STRUCTURE.withDetail(Component.translatable("gui.cft.job_detail.needs_structure",
                        LangUtil.structureName(structureType)));
    }

    public Optional<ScheduleDefinition> getSchedule() {
        return properties.schedule();
    }

    public ResourceLocation getRequiredStructureType() {
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
