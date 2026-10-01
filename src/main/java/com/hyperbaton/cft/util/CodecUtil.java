package com.hyperbaton.cft.util;

import com.mojang.serialization.Codec;

import java.util.List;

public final class CodecUtil {

    private CodecUtil() {
    }

    /** A list that can also be written as a single value, without brackets. It's written back as a list. */
    public static <T> Codec<List<T>> singleOrList(Codec<T> codec) {
        return Codec.withAlternative(codec.listOf(), codec.xmap(List::of, list -> list.get(0)));
    }
}
