package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.StructureNeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public class StructureNeed extends Need {

    public static final Codec<StructureNeed> STRUCTURE_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(StructureNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(StructureNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(StructureNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(StructureNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(StructureNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(StructureNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(StructureNeed::isHidden),
            Codec.STRING.fieldOf("structure_type").forGetter(StructureNeed::getStructureType),
            Codec.BOOL.optionalFieldOf("requires_usage", false).forGetter(StructureNeed::isRequiresUsage),
            Codec.INT.optionalFieldOf("search_radius", 64).forGetter(StructureNeed::getSearchRadius),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(StructureNeed::getIcon)
    ).apply(instance, StructureNeed::new));

    private final String structureType;
    private final boolean requiresUsage;
    private final int searchRadius;

    public StructureNeed(String id, double damage, double damageThreshold, double providedHappiness,
                         double satisfactionThreshold, double frequency, boolean hidden,
                         String structureType, boolean requiresUsage, int searchRadius,
                         Optional<ResourceLocation> icon) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden, icon);
        this.structureType = structureType;
        this.requiresUsage = requiresUsage;
        this.searchRadius = searchRadius;
    }

    public String getStructureType() {
        return structureType;
    }

    public boolean isRequiresUsage() {
        return requiresUsage;
    }

    public int getSearchRadius() {
        return searchRadius;
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.structure").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("bricks"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.STRUCTURE_NEED.get();
    }

    @Override
    public NeedSatisfier<StructureNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new StructureNeedSatisfier(satisfaction, isSatisfied, this);
    }
}
