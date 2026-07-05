package com.hyperbaton.cft.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * An attendance requirement for a ritual. A xoonglin counts toward every rule whose
 * class list contains its social class. The ritual can start when every rule's min
 * is reached, and attendance is capped so no rule's max is exceeded.
 */
public record AttendanceRule(List<String> classes, int min, int max) {

    public static final Codec<AttendanceRule> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.listOf().fieldOf("classes").forGetter(AttendanceRule::classes),
            Codec.INT.optionalFieldOf("min", 0).forGetter(AttendanceRule::min),
            Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(AttendanceRule::max)
    ).apply(inst, AttendanceRule::new));

    public boolean appliesTo(String socialClassId) {
        return classes.contains(socialClassId);
    }
}
