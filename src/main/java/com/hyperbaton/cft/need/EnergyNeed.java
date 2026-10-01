package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.EnergyNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class EnergyNeed extends Need {
    public static final Codec<EnergyNeed> ENERGY_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec(),
            Codec.INT.fieldOf("energy_amount").forGetter(EnergyNeed::getEnergyAmount)
    ).apply(instance, EnergyNeed::new));

    private final int energyAmount;

    public EnergyNeed(Properties properties, int energyAmount) {
        super(properties);
        this.energyAmount = energyAmount;
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.ENERGY_NEED.get();
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier() {
        return createSatisfier(this.getSatisfactionThreshold(), false);
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new EnergyNeedSatisfier(satisfaction, isSatisfied, this);
    }

    public int getEnergyAmount() {
        return energyAmount;
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.energy").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("redstone"));
    }
}
