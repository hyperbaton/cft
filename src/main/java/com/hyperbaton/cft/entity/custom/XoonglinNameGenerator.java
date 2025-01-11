package com.hyperbaton.cft.entity.custom;

import java.util.List;
import java.util.Random;

public class XoonglinNameGenerator {
    private static final List<String> PREFIXES = List.of("Xoo", "Zyn", "Glo", "Plon", "Flek", "Drim");
    private static final List<String> SUFFIXES = List.of("lin", "glar", "bo", "quix", "nib", "tron");

    private static final Random RANDOM = new Random();

    public static String generateName() {
        String prefix = PREFIXES.get(RANDOM.nextInt(PREFIXES.size()));
        String suffix = SUFFIXES.get(RANDOM.nextInt(SUFFIXES.size()));
        return prefix + suffix;
    }
}
