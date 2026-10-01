package com.hyperbaton.cft.socialclass;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinNameGenerator;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.entity.ai.schedule.ScheduleDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SocialClass {
    public static final Codec<SocialClass> SOCIAL_CLASS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("max_happiness").forGetter(SocialClass::getMaxHappiness),
            Codec.DOUBLE.fieldOf("mating_happiness_threshold").forGetter(SocialClass::getMatingHappinessThreshold),
            Codec.INT.fieldOf("spontaneously_spawn_population").forGetter(SocialClass::getSpontaneouslySpawnPopulation),
            ResourceLocation.CODEC.listOf().fieldOf("needs").forGetter(SocialClass::getNeeds),
            SocialClassUpdate.SOCIAL_CLASS_UPDATE_CODEC.listOf().fieldOf("upgrades").forGetter(SocialClass::getUpgrades),
            SocialClassUpdate.SOCIAL_CLASS_UPDATE_CODEC.listOf().fieldOf("downgrades").forGetter(SocialClass::getDowngrades),
            ResourceLocation.CODEC.listOf().optionalFieldOf("jobs", List.of()).forGetter(SocialClass::getJobs),
            Codec.BOOL.optionalFieldOf("can_upgrade_as_baby", false).forGetter(SocialClass::canUpgradeAsBaby),
            Codec.BOOL.optionalFieldOf("can_downgrade_as_baby", true).forGetter(SocialClass::canDowngradeAsBaby),
            Codec.INT.optionalFieldOf("mating_delay", -1).forGetter(SocialClass::getMatingDelay),
            Codec.DOUBLE.optionalFieldOf("max_health", 20.0).forGetter(SocialClass::getMaxHealth),
            Codec.STRING.listOf().optionalFieldOf("name_samples", List.of()).forGetter(SocialClass::getNameSamples),
            ScheduleDefinition.CODEC.optionalFieldOf("schedule").forGetter(SocialClass::getSchedule)
    ).apply(instance, SocialClass::new));

    private double maxHappiness;
    private double matingHappinessThreshold;
    private int spontaneouslySpawnPopulation;
    private List<ResourceLocation> needs;
    private final List<ResourceLocation> jobs;
    private List<SocialClassUpdate> upgrades;
    private List<SocialClassUpdate> downgrades;
    private final boolean canUpgradeAsBaby;
    private final boolean canDowngradeAsBaby;
    private final int matingDelay;
    private final double maxHealth;
    /** Sample names the name generator learns from; empty to use the default Xoonglin names. */
    private final List<String> nameSamples;
    /** Built on first use, since training the Markov chain isn't free and most classes may never need it. */
    private XoonglinNameGenerator nameGenerator;
    /** Daily routine for this class; null if its Xoonglins don't follow any. */
    private final ScheduleDefinition schedule;

    public SocialClass(double maxHappiness, double matingHappinessThreshold, int spontaneouslySpawnPopulation,
                       List<ResourceLocation> needs, List<SocialClassUpdate> upgrades, List<SocialClassUpdate> downgrades,
                       List<ResourceLocation> jobs, boolean canUpgradeAsBaby, boolean canDowngradeAsBaby,
                       int matingDelay, double maxHealth, List<String> nameSamples,
                       Optional<ScheduleDefinition> schedule) {
        this.maxHappiness = maxHappiness;
        this.matingHappinessThreshold = matingHappinessThreshold;
        this.spontaneouslySpawnPopulation = spontaneouslySpawnPopulation;
        this.needs = needs;
        this.jobs = jobs != null ? List.copyOf(jobs) : List.of();
        this.upgrades = upgrades;
        this.downgrades = downgrades;
        this.canUpgradeAsBaby = canUpgradeAsBaby;
        this.canDowngradeAsBaby = canDowngradeAsBaby;
        this.matingDelay = matingDelay;
        this.maxHealth = maxHealth;
        this.nameSamples = nameSamples != null ? List.copyOf(nameSamples) : List.of();
        this.schedule = schedule.orElse(null);
    }

    public double getMaxHappiness() {
        return maxHappiness;
    }

    public void setMaxHappiness(double maxHappiness) {
        this.maxHappiness = maxHappiness;
    }

    public double getMatingHappinessThreshold() {
        return matingHappinessThreshold;
    }

    public void setMatingHappinessThreshold(double matingHappinessThreshold) {
        this.matingHappinessThreshold = matingHappinessThreshold;
    }

    public int getSpontaneouslySpawnPopulation() {
        return spontaneouslySpawnPopulation;
    }

    public void setSpontaneouslySpawnPopulation(int spontaneouslySpawnPopulation) {
        this.spontaneouslySpawnPopulation = spontaneouslySpawnPopulation;
    }

    public List<ResourceLocation> getNeeds() {
        return needs;
    }

    public void setNeeds(List<ResourceLocation> needs) {
        this.needs = needs;
    }

    public List<SocialClassUpdate> getUpgrades() {
        return upgrades;
    }

    public void setUpgrades(List<SocialClassUpdate> upgrades) {
        this.upgrades = upgrades;
    }

    public List<SocialClassUpdate> getDowngrades() {
        return downgrades;
    }

    public void setDowngrades(List<SocialClassUpdate> downgrades) {
        this.downgrades = downgrades;
    }

    public List<ResourceLocation> getJobs() {
        return jobs;
    }

    /**
     * Filters this class's job list down to those eligible for the given age
     * (baby/adult), per the job's available_to_babies/available_to_adults fields.
     */
    public List<ResourceLocation> getJobsForAge(boolean isBaby) {
        return jobs.stream()
                .filter(jobId -> {
                    Job job = CftRegistry.JOBS.get(jobId);
                    return job != null && (isBaby ? job.isAvailableToBabies() : job.isAvailableToAdults());
                })
                .toList();
    }

    /**
     * Picks a random job from this class's list that the given age (baby/adult) is
     * eligible for, per the job's available_to_babies/available_to_adults fields.
     * Returns null if no eligible job exists (the Xoonglin remains jobless).
     */
    public ResourceLocation getRandomJob(RandomSource random, boolean isBaby) {
        List<ResourceLocation> eligible = getJobsForAge(isBaby);
        if (eligible.isEmpty()) return null;
        return eligible.get(random.nextInt(eligible.size()));
    }

    public boolean canUpgradeAsBaby() {
        return canUpgradeAsBaby;
    }

    public boolean canDowngradeAsBaby() {
        return canDowngradeAsBaby;
    }

    public int getMatingDelay() {
        return matingDelay;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public Optional<ScheduleDefinition> getSchedule() {
        return Optional.ofNullable(schedule);
    }

    public List<String> getNameSamples() {
        return nameSamples;
    }

    /** A new name for a Xoonglin of this class, based on its name samples. */
    public String generateName() {
        if (nameSamples.isEmpty()) {
            return XoonglinNameGenerator.generateName();
        }
        if (nameGenerator == null) {
            nameGenerator = new XoonglinNameGenerator(nameSamples);
        }
        return nameGenerator.generate();
    }
}
