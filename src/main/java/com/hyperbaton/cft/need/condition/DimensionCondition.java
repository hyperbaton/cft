package com.hyperbaton.cft.need.condition;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.util.CodecUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/** The Xoonglin is in one of the given dimensions, such as {@code minecraft:the_nether}. */
public record DimensionCondition(List<ResourceKey<Level>> dimensions) implements NeedCondition {

    public static final Codec<DimensionCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtil.singleOrList(ResourceKey.codec(Registries.DIMENSION)).fieldOf("dimensions")
                    .forGetter(DimensionCondition::dimensions)
    ).apply(instance, DimensionCondition::new));

    @Override
    public boolean test(XoonglinEntity xoonglin) {
        return dimensions.contains(xoonglin.level().dimension());
    }

    @Override
    public Codec<? extends NeedCondition> conditionType() {
        return CftRegistry.DIMENSION_CONDITION.get();
    }
}
