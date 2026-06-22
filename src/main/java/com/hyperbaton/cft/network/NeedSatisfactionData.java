package com.hyperbaton.cft.network;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class NeedSatisfactionData {
    public final double satisfaction;
    public final double damageThreshold;
    public final double satisfactionThreshold;
    public final List<ResourceLocation> icons;

    public NeedSatisfactionData(double satisfaction, double damageThreshold, double satisfactionThreshold,
                                List<ResourceLocation> icons) {
        this.satisfaction = satisfaction;
        this.damageThreshold = damageThreshold;
        this.satisfactionThreshold = satisfactionThreshold;
        this.icons = icons;
    }
}
