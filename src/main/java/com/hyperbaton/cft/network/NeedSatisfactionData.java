package com.hyperbaton.cft.network;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class NeedSatisfactionData {
    public final double satisfaction;
    public final double damageThreshold;
    public final double satisfactionThreshold;
    public final List<ResourceLocation> icons;
    /** Extra text shown on hover over the need's icon (e.g. which book is wanted). Nullable. */
    public final String extraTooltip;
    /** Whether the need applies right now; one that doesn't is shown greyed out. */
    public final boolean active;

    public NeedSatisfactionData(double satisfaction, double damageThreshold, double satisfactionThreshold,
                                List<ResourceLocation> icons, String extraTooltip, boolean active) {
        this.satisfaction = satisfaction;
        this.damageThreshold = damageThreshold;
        this.satisfactionThreshold = satisfactionThreshold;
        this.icons = icons;
        this.extraTooltip = extraTooltip;
        this.active = active;
    }
}
