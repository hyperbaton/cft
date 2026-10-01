package com.hyperbaton.cft.need;

import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.Registries;
import com.hyperbaton.cft.util.RegistryEntries;
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
            propertiesCodec(),
            RegistryEntries.codec(Registries.ENTITY_TYPE).fieldOf("entity_types").forGetter(PetNeed::getEntityTypes),
            Codec.INT.fieldOf("min_count").forGetter(PetNeed::getMinCount),
            Codec.INT.fieldOf("max_count").forGetter(PetNeed::getMaxCount),
            Codec.INT.fieldOf("radius").forGetter(PetNeed::getRadius)
    ).apply(instance, PetNeed::new));

    private final RegistryEntries<EntityType<?>> entityTypes;
    private final int minCount;
    private final int maxCount;
    private final int radius;

    public PetNeed(Properties properties, RegistryEntries<EntityType<?>> entityTypes, int minCount,
                   int maxCount, int radius) {
        super(properties);
        this.entityTypes = entityTypes;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.radius = radius;
    }

    public RegistryEntries<EntityType<?>> getEntityTypes() {
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
