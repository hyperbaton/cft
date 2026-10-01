package com.hyperbaton.cft.need.condition;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** At least one of the given conditions holds (a need's {@code active_when} list needs all of them). */
public record AnyOfCondition(List<NeedCondition> conditions) implements NeedCondition {

    public static final Codec<AnyOfCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NeedCondition.CODEC.listOf().fieldOf("conditions").forGetter(AnyOfCondition::conditions)
    ).apply(instance, AnyOfCondition::new));

    @Override
    public boolean test(XoonglinEntity xoonglin) {
        return conditions.stream().anyMatch(condition -> condition.test(xoonglin));
    }

    @Override
    public Codec<? extends NeedCondition> conditionType() {
        return CftRegistry.ANY_OF_CONDITION.get();
    }
}
