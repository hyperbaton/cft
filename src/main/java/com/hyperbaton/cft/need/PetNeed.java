package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.PetNeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class PetNeed extends Need {
    public static final Codec<PetNeed> PET_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(PetNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(PetNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(PetNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(PetNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(PetNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(PetNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(PetNeed::isHidden),
            ResourceLocation.CODEC.listOf().fieldOf("entity_types").forGetter(PetNeed::getEntityTypes),
            Codec.INT.fieldOf("min_count").forGetter(PetNeed::getMinCount),
            Codec.INT.fieldOf("max_count").forGetter(PetNeed::getMaxCount),
            Codec.INT.fieldOf("radius").forGetter(PetNeed::getRadius)
    ).apply(instance, PetNeed::new));

    private final List<ResourceLocation> entityTypes;
    private final int minCount;
    private final int maxCount;
    private final int radius;

    public PetNeed(String id, double damage, double damageThreshold, double providedHappiness,
                   double satisfactionThreshold, double frequency, boolean hidden,
                   List<ResourceLocation> entityTypes, int minCount, int maxCount, int radius) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden);
        this.entityTypes = List.copyOf(entityTypes);
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.radius = radius;
    }

    public List<ResourceLocation> getEntityTypes() {
        return entityTypes;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int getRadius() {
        return radius;
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.pet").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("lead"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.PET_NEED.get();
    }

    @Override
    public NeedSatisfier<PetNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new PetNeedSatisfier(satisfaction, isSatisfied, this);
    }
}
