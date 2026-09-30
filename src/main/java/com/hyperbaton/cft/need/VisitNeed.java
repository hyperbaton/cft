package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.job.data.ItemQuantity;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.VisitNeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * The Xoonglin wants to go to a structure of some type in its free time (a tavern, a market,
 * a plaza...) and spend a while there. How recently it went is just the need wearing off.
 */
public class VisitNeed extends Need {

    public static final Codec<VisitNeed> VISIT_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("damage").forGetter(VisitNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(VisitNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(VisitNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(VisitNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(VisitNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(VisitNeed::isHidden),
            Codec.BOOL.optionalFieldOf("bonus", DEFAULT_BONUS).forGetter(VisitNeed::isBonus),
            Codec.STRING.fieldOf("required_structure").forGetter(VisitNeed::getRequiredStructure),
            Codec.INT.optionalFieldOf("search_radius", 64).forGetter(VisitNeed::getSearchRadius),
            Codec.INT.optionalFieldOf("stay_duration", 1200).forGetter(VisitNeed::getStayDuration),
            ItemQuantity.CODEC.listOf().optionalFieldOf("consumes", List.of()).forGetter(VisitNeed::getConsumes),
            Codec.BOOL.optionalFieldOf("use_supplies", false).forGetter(VisitNeed::isUseSupplies),
            Codec.BOOL.optionalFieldOf("requires_running", false).forGetter(VisitNeed::isRequiresRunning),
            Codec.STRING.listOf().optionalFieldOf("running_work_steps", List.of()).forGetter(VisitNeed::getRunningWorkSteps),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(VisitNeed::getIcon)
    ).apply(instance, VisitNeed::new));

    private final String requiredStructure;
    private final int searchRadius;
    private final int stayDuration;
    private final List<ItemQuantity> consumes;
    private final boolean useSupplies;
    private final boolean requiresRunning;
    private final List<String> runningWorkSteps;

    public VisitNeed(double damage, double damageThreshold, double providedHappiness,
                     double satisfactionThreshold, double frequency, boolean hidden, boolean bonus,
                     String requiredStructure, int searchRadius, int stayDuration, List<ItemQuantity> consumes,
                     boolean useSupplies, boolean requiresRunning, List<String> runningWorkSteps,
                     Optional<ResourceLocation> icon) {
        super(damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden, bonus, icon);
        this.requiredStructure = requiredStructure;
        this.searchRadius = searchRadius;
        this.stayDuration = stayDuration;
        this.consumes = List.copyOf(consumes);
        this.useSupplies = useSupplies;
        this.requiresRunning = requiresRunning;
        this.runningWorkSteps = List.copyOf(runningWorkSteps);
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.visit").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("bell"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.VISIT_NEED.get();
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new VisitNeedSatisfier(satisfaction, isSatisfied, this);
    }

    /** Structure type to visit. */
    public String getRequiredStructure() {
        return requiredStructure;
    }

    /** How far from the Xoonglin's home (Manhattan distance) the structure may be. */
    public int getSearchRadius() {
        return searchRadius;
    }

    /** How long, in ticks, the Xoonglin stays at the structure once it arrives. */
    public int getStayDuration() {
        return stayDuration;
    }

    /**
     * Goods taken from the structure's containers on arrival, like a drink at the tavern. Only
     * structures that hold all of them are visited.
     */
    public List<ItemQuantity> getConsumes() {
        return consumes;
    }

    /**
     * Whether, while visiting, the Xoonglin can use the structure's containers to satisfy its
     * goods, fluid and energy needs, as it does with the ones in its home.
     */
    public boolean isUseSupplies() {
        return useSupplies;
    }

    /** Whether only running structures are visited: one of their workers is working right now. */
    public boolean isRequiresRunning() {
        return requiresRunning;
    }

    /** With {@link #isRequiresRunning()}, the work steps the worker must be on; empty for any. */
    public List<String> getRunningWorkSteps() {
        return runningWorkSteps;
    }
}
