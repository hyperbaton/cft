package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.EnergyNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

public class EnergyNeed extends Need {
    public static final Codec<EnergyNeed> ENERGY_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(EnergyNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(EnergyNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(EnergyNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(EnergyNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(EnergyNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(EnergyNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(EnergyNeed::isHidden),
            Codec.INT.fieldOf("energy_amount").forGetter(EnergyNeed::getEnergyAmount)
    ).apply(instance, EnergyNeed::new));

    private final int energyAmount;

    public static final String TAG_ENERGY_AMOUNT = "energy_amount";

    public EnergyNeed(String id, double damage, double damageThreshold, double providedHappiness,
                      double satisfactionThreshold, double frequency, boolean hidden,
                      int energyAmount) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden);
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
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();
        tag.putInt(TAG_ENERGY_AMOUNT, energyAmount);
        return tag;
    }
}
