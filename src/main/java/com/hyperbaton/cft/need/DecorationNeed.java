package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.DecorationNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

public class DecorationNeed extends Need {

    public static final Codec<DecorationNeed> DECORATION_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(DecorationNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(DecorationNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(DecorationNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(DecorationNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(DecorationNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(DecorationNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(DecorationNeed::isHidden),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block").forGetter(d -> Optional.ofNullable(d.getBlock())),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("block_tag").forGetter(d -> Optional.ofNullable(d.getBlockTag())),
            Codec.INT.fieldOf("min_count").forGetter(DecorationNeed::getMinCount),
            Codec.INT.fieldOf("radius").forGetter(DecorationNeed::getRadius),
            Codec.DOUBLE.optionalFieldOf("min_spread", 0.0).forGetter(DecorationNeed::getMinSpread),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(DecorationNeed::getIcon)
    ).apply(instance, DecorationNeed::new));

    private final Block block;
    private final TagKey<Block> blockTag;
    private final int minCount;
    private final int radius;
    private final double minSpread;

    public DecorationNeed(String id, double damage, double damageThreshold, double providedHappiness,
                          double satisfactionThreshold, double frequency, boolean hidden,
                          Optional<Block> block, Optional<TagKey<Block>> blockTag,
                          int minCount, int radius, double minSpread,
                          Optional<ResourceLocation> icon) {
        super(id, damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden, icon);
        this.block = block.orElse(null);
        this.blockTag = blockTag.orElse(null);
        this.minCount = minCount;
        this.radius = radius;
        this.minSpread = Math.max(0.0, Math.min(1.0, minSpread));
    }

    public Block getBlock() {
        return block;
    }

    public TagKey<Block> getBlockTag() {
        return blockTag;
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
        if (block != null) {
            return List.of(BuiltInRegistries.BLOCK.getKey(block));
        }
        return List.of(ResourceLocation.withDefaultNamespace("flower_pot"));
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
