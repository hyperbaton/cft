package com.hyperbaton.cft.util;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces nonsense-but-prose-shaped titles and page text for writer-produced books.
 * Deliberately simple (fixed word banks + sentence templates, no learned/AI text
 * generation): this is flavor text, not something worth a runtime dependency for.
 */
public final class BookTextGenerator {
    private BookTextGenerator() {}

    private static final int MAX_PAGE_LENGTH = 200;

    private static final String[] NOUNS = {
            "miller", "lantern", "hollow", "raven", "ferry", "orchard", "well", "cairn",
            "harbor", "meadow", "chronicle", "shepherd", "quarry", "ember", "loom",
            "wanderer", "grove", "tide", "forge", "wren"
    };
    private static final String[] ADJECTIVES = {
            "quiet", "restless", "hollow", "distant", "faded", "stubborn", "gilded",
            "weary", "unspoken", "crooked", "silver", "brittle", "patient", "wandering"
    };
    private static final String[] VERBS = {
            "spoke of", "waited for", "forgot", "carried", "outlived", "circled",
            "mistook", "remembered", "abandoned", "followed"
    };
    private static final String[] PLACES = {
            "the valley", "the old road", "the harbor town", "the north fields",
            "the empty house", "the market square", "the long winter", "the last village"
    };

    private static final String[] TITLE_TEMPLATES = {
            "The %s of the %s %s",
            "%s and the %s %s",
            "A %s %s",
            "The %s %s",
    };

    private static final String[] SENTENCE_TEMPLATES = {
            "The %s %s %s the %s %s.",
            "%s was never %s in %s.",
            "No one %s the %s %s again.",
            "In %s, the %s %s stayed %s.",
    };

    public static String generateTitle(RandomSource random) {
        String template = pick(random, TITLE_TEMPLATES);
        String title = fill(template, random);
        return capitalizeWords(title);
    }

    public static List<String> generatePages(RandomSource random, int pageCount) {
        List<String> pages = new ArrayList<>();
        for (int i = 0; i < Math.max(1, pageCount); i++) {
            pages.add(generatePage(random));
        }
        return pages;
    }

    private static String generatePage(RandomSource random) {
        StringBuilder page = new StringBuilder();
        int sentences = 4 + random.nextInt(5);
        // Stop as soon as the threshold is reached, but keep the last sentence whole
        for (int i = 0; i < sentences && page.length() < MAX_PAGE_LENGTH; i++) {
            String sentence = fill(pick(random, SENTENCE_TEMPLATES), random);
            sentence = Character.toUpperCase(sentence.charAt(0)) + sentence.substring(1);
            if (!page.isEmpty()) page.append(' ');
            page.append(sentence);
        }
        return page.toString();
    }

    /** Fills every %s placeholder in the template from a word bank chosen by rotation. */
    private static String fill(String template, RandomSource random) {
        StringBuilder result = new StringBuilder();
        int wordIndex = 0;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '%' && i + 1 < template.length() && template.charAt(i + 1) == 's') {
                result.append(nextWord(random, wordIndex++));
                i++;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String nextWord(RandomSource random, int wordIndex) {
        return switch (wordIndex % 4) {
            case 0 -> pick(random, ADJECTIVES);
            case 1 -> pick(random, NOUNS);
            case 2 -> pick(random, VERBS);
            default -> pick(random, PLACES);
        };
    }

    private static String pick(RandomSource random, String[] bank) {
        return bank[random.nextInt(bank.length)];
    }

    private static String capitalizeWords(String text) {
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
