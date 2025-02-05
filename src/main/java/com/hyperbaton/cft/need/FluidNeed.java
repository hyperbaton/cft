package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.FluidNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraftforge.fluids.FluidStack;

public class FluidNeed extends Need {
    public static final Codec<FluidNeed> FLUID_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(FluidNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(FluidNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(FluidNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(FluidNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(FluidNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(FluidNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(FluidNeed::isHidden),
            FluidStack.CODEC.fieldOf("fluid_stack").forGetter(FluidNeed::getFluidStack)
    ).apply(instance, FluidNeed::new));

    private final FluidStack fluidStack;

    public FluidNeed(String id, double damage, double damageThreshold, double providedHappiness,
                     double satisfactionThreshold, double frequency, boolean hidden,
                     FluidStack fluidStack) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden);
        this.fluidStack = fluidStack;
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.FLUID_NEED.get();
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier() {
        return createSatisfier(this.getSatisfactionThreshold(), false);
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new FluidNeedSatisfier(satisfaction, isSatisfied, this);
    }

    public FluidStack getFluidStack() {
        return fluidStack;
    }

}
