package com.hyperbaton.cft.need.condition;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** The given condition doesn't hold. */
public record NotCondition(NeedCondition condition) implements NeedCondition {

    public static final Codec<NotCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NeedCondition.CODEC.fieldOf("condition").forGetter(NotCondition::condition)
    ).apply(instance, NotCondition::new));

    @Override
    public boolean test(XoonglinEntity xoonglin) {
        return !condition.test(xoonglin);
    }

    @Override
    public Codec<? extends NeedCondition> conditionType() {
        return CftRegistry.NOT_CONDITION.get();
    }
}
