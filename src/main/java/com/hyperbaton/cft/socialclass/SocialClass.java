package com.hyperbaton.cft.socialclass;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.job.Job;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SocialClass {
    public static final Codec<SocialClass> SOCIAL_CLASS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(SocialClass::getId),
            Codec.DOUBLE.fieldOf("maxHappiness").forGetter(SocialClass::getMaxHappiness),
            Codec.DOUBLE.fieldOf("matingHappinessThreshold").forGetter(SocialClass::getMatingHappinessThreshold),
            Codec.INT.fieldOf("spontaneouslySpawnPopulation").forGetter(SocialClass::getSpontaneouslySpawnPopulation),
            Codec.STRING.listOf().fieldOf("needs").forGetter(SocialClass::getNeeds),
            SocialClassUpdate.SOCIAL_CLASS_UPDATE_CODEC.listOf().fieldOf("upgrades").forGetter(SocialClass::getUpgrades),
            SocialClassUpdate.SOCIAL_CLASS_UPDATE_CODEC.listOf().fieldOf("downgrades").forGetter(SocialClass::getDowngrades),
            ResourceLocation.CODEC.listOf().optionalFieldOf("jobs", List.of()).forGetter(SocialClass::getJobs),
            Codec.BOOL.optionalFieldOf("canUpgradeAsBaby", false).forGetter(SocialClass::canUpgradeAsBaby),
            Codec.BOOL.optionalFieldOf("canDowngradeAsBaby", true).forGetter(SocialClass::canDowngradeAsBaby),
            Codec.INT.optionalFieldOf("matingDelay", -1).forGetter(SocialClass::getMatingDelay),
            Codec.DOUBLE.optionalFieldOf("maxHealth", 20.0).forGetter(SocialClass::getMaxHealth)
    ).apply(instance, SocialClass::new));

    private String id;
    private double maxHappiness;
    private double matingHappinessThreshold;
    private int spontaneouslySpawnPopulation;
    private List<String> needs;
    private final List<ResourceLocation> jobs;
    private List<SocialClassUpdate> upgrades;
    private List<SocialClassUpdate> downgrades;
    private final boolean canUpgradeAsBaby;
    private final boolean canDowngradeAsBaby;
    private final int matingDelay;
    private final double maxHealth;

    public SocialClass(String id, double maxHappiness, double matingHappinessThreshold, int spontaneouslySpawnPopulation,
                       List<String> needs, List<SocialClassUpdate> upgrades, List<SocialClassUpdate> downgrades,
                       List<ResourceLocation> jobs, boolean canUpgradeAsBaby, boolean canDowngradeAsBaby,
                       int matingDelay, double maxHealth) {
        this.id = id;
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
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<String> getNeeds() {
        return needs;
    }

    public void setNeeds(List<String> needs) {
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
}
