package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.SleepNeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * The Xoonglin needs to sleep in a bed inside its home. It goes to bed during the rest time of
 * its schedule or, if it has no schedule, at night.
 */
public class SleepNeed extends Need {

    public static final Codec<SleepNeed> SLEEP_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(SleepNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(SleepNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(SleepNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(SleepNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(SleepNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(SleepNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(SleepNeed::isHidden),
            Codec.BOOL.optionalFieldOf("bonus", DEFAULT_BONUS).forGetter(SleepNeed::isBonus),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(SleepNeed::getIcon)
    ).apply(instance, SleepNeed::new));

    public SleepNeed(String id, double damage, double damageThreshold, double providedHappiness,
                     double satisfactionThreshold, double frequency, boolean hidden, boolean bonus,
                     Optional<ResourceLocation> icon) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden, bonus, icon);
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.sleep").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("red_bed"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.SLEEP_NEED.get();
    }

    @Override
    public NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new SleepNeedSatisfier(satisfaction, isSatisfied, this);
    }
}
