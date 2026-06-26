package com.hyperbaton.cft.socialclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.List;
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

    public ResourceLocation getRandomJob(RandomSource random) {
        if (jobs.isEmpty()) return null;
        return jobs.get(random.nextInt(jobs.size()));
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
