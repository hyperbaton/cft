package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.HomeNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class HomeNeed extends Need {

    public static final Codec<HomeNeed> HOME_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec(),
            ResourceLocation.CODEC.fieldOf("required_structure").forGetter(HomeNeed::getRequiredStructure)
    ).apply(instance, HomeNeed::new));

    private final ResourceLocation requiredStructure;

    public HomeNeed(Properties properties, ResourceLocation requiredStructure) {
        super(properties);
        this.requiredStructure = requiredStructure;
    }

    public ResourceLocation getRequiredStructure() {
        return requiredStructure;
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.home").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("oak_door"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.HOME_NEED.get();
    }

    @Override
    public NeedSatisfier<HomeNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new HomeNeedSatisfier(satisfaction, isSatisfied, this);
    }
}
