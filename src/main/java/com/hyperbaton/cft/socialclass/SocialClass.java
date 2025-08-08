package com.hyperbaton.cft.socialclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

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
            ResourceLocation.CODEC.optionalFieldOf("job").forGetter(socialClass -> Optional.ofNullable(socialClass.getJob()))
    ).apply(instance, SocialClass::new));

    /**
     * Identifier in the format of a resource location. It must coincide with the placement of the social class file
     * to be properly loaded.
     * Example: "cft:citizen"
     */
    private String id;
    private double maxHappiness;

    private double matingHappinessThreshold;
    private int spontaneouslySpawnPopulation;
    private List<String> needs;
    private final ResourceLocation job;
    private List<SocialClassUpdate> upgrades;
    private List<SocialClassUpdate> downgrades;

    public SocialClass(String id, double maxHappiness, double matingHappinessThreshold, int spontaneouslySpawnPopulation,
                       List<String> needs, List<SocialClassUpdate> upgrades, List<SocialClassUpdate> downgrades, Optional<ResourceLocation> job) {
        this.id = id;
        this.maxHappiness = maxHappiness;
        this.matingHappinessThreshold = matingHappinessThreshold;
        this.spontaneouslySpawnPopulation = spontaneouslySpawnPopulation;
        this.needs = needs;
        this.job = job.orElse(null);
        this.upgrades = upgrades;
        this.downgrades = downgrades;
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

    public ResourceLocation getJob() {
        return job;
    }
}
