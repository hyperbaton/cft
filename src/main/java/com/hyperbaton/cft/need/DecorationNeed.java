package com.hyperbaton.cft.need;

import com.hyperbaton.cft.util.RegistryEntries;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.DecorationNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class DecorationNeed extends Need {

    public static final Codec<DecorationNeed> DECORATION_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec(),
            RegistryEntries.codec(Registries.BLOCK).fieldOf("block").forGetter(DecorationNeed::getBlock),
            Codec.INT.fieldOf("min_count").forGetter(DecorationNeed::getMinCount),
            Codec.INT.fieldOf("radius").forGetter(DecorationNeed::getRadius),
            Codec.DOUBLE.optionalFieldOf("min_spread", 0.0).forGetter(DecorationNeed::getMinSpread)
    ).apply(instance, DecorationNeed::new));

    private final RegistryEntries<Block> block;
    private final int minCount;
    private final int radius;
    private final double minSpread;

    public DecorationNeed(Properties properties, RegistryEntries<Block> block, int minCount, int radius,
                          double minSpread) {
        super(properties);
        this.block = block;
        this.minCount = minCount;
        this.radius = radius;
        this.minSpread = Math.max(0.0, Math.min(1.0, minSpread));
    }

    public RegistryEntries<Block> getBlock() {
        return block;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getRadius() {
        return radius;
    }

    public double getMinSpread() {
        return minSpread;
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.decoration").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return block.ids().stream().findFirst()
                .map(List::of)
                .orElseGet(() -> List.of(ResourceLocation.withDefaultNamespace("flower_pot")));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.DECORATION_NEED.get();
    }

    @Override
    public NeedSatisfier<DecorationNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new DecorationNeedSatisfier(satisfaction, isSatisfied, this);
    }
}
