package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.ReadingNeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Wants a book from the world's roster of writer-produced works to be present in the
 * Xoonglin's home. Which book is "wanted" is picked fresh each check (deterministically,
 * from the current roster) rather than assigned and remembered — delivery is entirely up
 * to the player, this need only checks presence.
 */
public class ReadingNeed extends Need {

    public static final Codec<ReadingNeed> READING_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(ReadingNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(ReadingNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(ReadingNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(ReadingNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(ReadingNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(ReadingNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(ReadingNeed::isHidden),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(ReadingNeed::getIcon)
    ).apply(instance, ReadingNeed::new));

    public ReadingNeed(String id, double damage, double damageThreshold, double providedHappiness,
                       double satisfactionThreshold, double frequency, boolean hidden,
                       Optional<ResourceLocation> icon) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden, icon);
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.READING_NEED.get();
    }

    @Override
    public NeedSatisfier<ReadingNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new ReadingNeedSatisfier(satisfaction, isSatisfied, this);
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.reading").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("written_book"));
    }
}
