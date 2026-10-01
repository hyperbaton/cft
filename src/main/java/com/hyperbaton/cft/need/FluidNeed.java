package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.FluidNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class FluidNeed extends Need {
    public static final Codec<FluidNeed> FLUID_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec(),
            FluidStack.CODEC.fieldOf("fluid_stack").forGetter(FluidNeed::getFluidStack)
    ).apply(instance, FluidNeed::new));

    private final FluidStack fluidStack;

    public FluidNeed(Properties properties, FluidStack fluidStack) {
        super(properties);
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

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.fluid").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluidStack.getFluid());
        ResourceLocation bucketId = ResourceLocation.fromNamespaceAndPath(fluidId.getNamespace(), fluidId.getPath() + "_bucket");
        if (BuiltInRegistries.ITEM.containsKey(bucketId)) {
            return List.of(bucketId);
        }
        return List.of(fluidId);
    }
}
