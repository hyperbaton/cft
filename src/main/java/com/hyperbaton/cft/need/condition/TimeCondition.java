package com.hyperbaton.cft.need.condition;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Between two times of the day, in ticks (0 is sunrise, 6000 noon, 12000 sunset, 18000 midnight):
 * from {@code from} until just before {@code to}. If {@code to} is the smaller one, the range goes
 * past midnight, so 12000 to 0 is the night.
 */
public record TimeCondition(int from, int to) implements NeedCondition {

    public static final Codec<TimeCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 23999).fieldOf("from").forGetter(TimeCondition::from),
            Codec.intRange(0, 23999).fieldOf("to").forGetter(TimeCondition::to)
    ).apply(instance, TimeCondition::new));

    @Override
    public boolean test(XoonglinEntity xoonglin) {
        long time = xoonglin.level().getDayTime() % 24000L;
        return from <= to
                ? time >= from && time < to
                : time >= from || time < to;
    }

    @Override
    public Codec<? extends NeedCondition> conditionType() {
        return CftRegistry.TIME_CONDITION.get();
    }
}
