package com.hyperbaton.cft.util;

import com.hyperbaton.cft.job.TextBank;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces nonsense-but-prose-shaped titles and page text for writer-produced books, by
 * filling a WriterJob's own TextBank templates.
 */
public final class BookTextGenerator {
    private BookTextGenerator() {}

    private static final int MAX_PAGE_LENGTH = 200;

    public static String generateTitle(RandomSource random, TextBank bank) {
        String template = pick(random, bank.titleTemplates());
        if (template == null) return "Untitled";
        String title = fill(template, random, bank);
        return capitalizeWords(title);
    }

    public static List<String> generatePages(RandomSource random, int pageCount, TextBank bank) {
        List<String> pages = new ArrayList<>();
        for (int i = 0; i < Math.max(1, pageCount); i++) {
            pages.add(generatePage(random, bank));
        }
        return pages;
    }

    private static String generatePage(RandomSource random, TextBank bank) {
        StringBuilder page = new StringBuilder();
        int sentences = 4 + random.nextInt(5);
        // Stop as soon as the threshold is reached, but always keep the sentence that
        // reached it whole rather than cutting mid-phrase.
        for (int i = 0; i < sentences && page.length() < MAX_PAGE_LENGTH; i++) {
            String template = pick(random, bank.sentenceTemplates());
            if (template == null) break;
            String sentence = fill(template, random, bank);
            if (sentence.isEmpty()) continue;
            sentence = Character.toUpperCase(sentence.charAt(0)) + sentence.substring(1);
            if (!page.isEmpty()) page.append(' ');
            page.append(sentence);
        }
        return page.toString();
    }

    /** Fills every %category% placeholder in the template with a random word from the bank. */
    private static String fill(String template, RandomSource random, TextBank bank) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '%') {
                int end = template.indexOf('%', i + 1);
                if (end != -1) {
                    String category = template.substring(i + 1, end);
                    String word = bank.pickWord(category, random);
                    if (word != null) {
                        result.append(word);
                        i = end + 1;
                        continue;
                    }
                }
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

    private static String pick(RandomSource random, List<String> options) {
        if (options == null || options.isEmpty()) return null;
        return options.get(random.nextInt(options.size()));
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
