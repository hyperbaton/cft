package com.hyperbaton.cft.entity.custom;

import java.util.*;

/**
 * Generates names with a Markov chain trained on a list of sample names, so the results
 * sound like the samples without copying them. Each social class can provide its own
 * samples (e.g. a different "culture" per class); otherwise the default Xoonglin-style
 * samples are used.
 */
public class XoonglinNameGenerator {
    private static final Random RANDOM = new Random();

    // Training data - a collection of Xoonglin-style names to learn patterns from
    public static final List<String> DEFAULT_SAMPLES = List.of(
            "zynara", "glompix", "plonbo", "flekquix", "drimnib",
            "zylphy", "goolin", "plixar", "flembo", "dryglin", "xynquix",
            "glorbix", "plonyx", "flekrin", "zynolin", "drimple", "xooglix",
            "plynbo", "flemrix", "dryglix", "zynple", "gloorin", "plixbo",
            "fleklyn", "drimglix", "xoobrix", "zynrix", "gloplin", "plynrix",
            "flemglix", "drygrin", "zynbrix", "gloolin", "plixlyn", "flekglix",
            "drimrix", "xoogrin", "plynlin", "flemrix", "drybrix", "zynglix",
            "gloorix", "plixglix", "flekbrix", "drimglin", "xoolin", "zyngrin",
            "gloobrix", "plynglix", "flemgrin", "dryblin", "zynolin", "gloogrix",
            "plixbrix", "flekgrin", "drimolin", "xoogbrix", "zyngbrix", "glooglin"
    );

    public static final XoonglinNameGenerator DEFAULT = new XoonglinNameGenerator(DEFAULT_SAMPLES);

    // Markov chain order (how many previous characters to consider)
    private static final int CHAIN_ORDER = 2;
    private static final int MAX_TRIES = 10;
    /** How much longer than the longest sample a generated name may grow. */
    private static final int EXTRA_LENGTH = 3;
    private static final char START = '^';
    private static final char END = '$';

    private final List<String> samples;
    // Character transition probabilities
    private final Map<String, List<Character>> transitions = new HashMap<>();
    private final int minLength;
    private final int maxLength;

    /**
     * @param samples names to learn from. Blank entries are ignored; if none is left, the
     *                default samples are used instead.
     */
    public XoonglinNameGenerator(List<String> samples) {
        List<String> cleaned = samples.stream()
                .map(String::trim)
                .filter(sample -> !sample.isEmpty())
                .map(sample -> sample.toLowerCase(Locale.ROOT))
                .toList();
        this.samples = cleaned.isEmpty() ? DEFAULT_SAMPLES : cleaned;
        this.minLength = Math.min(3, this.samples.stream().mapToInt(String::length).min().orElse(3));
        this.maxLength = this.samples.stream().mapToInt(String::length).max().orElse(10) + EXTRA_LENGTH;
        buildMarkovChain();
    }

    private void buildMarkovChain() {
        String startMarkers = String.valueOf(START).repeat(CHAIN_ORDER);
        for (String name : samples) {
            String processedName = startMarkers + name + END; // Add start/end markers

            // Build transitions for each n-gram
            for (int i = 0; i + CHAIN_ORDER < processedName.length(); i++) {
                String state = processedName.substring(i, i + CHAIN_ORDER);
                char nextChar = processedName.charAt(i + CHAIN_ORDER);
                transitions.computeIfAbsent(state, k -> new ArrayList<>()).add(nextChar);
            }
        }
    }

    /** Uses the default Xoonglin-style samples. */
    public static String generateName() {
        return DEFAULT.generate();
    }

    public String generate() {
        for (int i = 0; i < MAX_TRIES; i++) {
            String name = walkChain();
            if (name.length() >= minLength) {
                return capitalizeFirst(name);
            }
        }
        // Fallback if the chain keeps producing names that are too short
        return capitalizeFirst(samples.get(RANDOM.nextInt(samples.size())));
    }

    private String walkChain() {
        StringBuilder name = new StringBuilder();
        String currentState = String.valueOf(START).repeat(CHAIN_ORDER);

        while (name.length() < maxLength) {
            List<Character> possibleNext = transitions.get(currentState);
            if (possibleNext == null || possibleNext.isEmpty()) {
                break;
            }

            char nextChar = possibleNext.get(RANDOM.nextInt(possibleNext.size()));
            if (nextChar == END) {
                break;
            }
            name.append(nextChar);

            // Update state (sliding window)
            currentState = currentState.substring(1) + nextChar;
        }
        return name.toString();
    }

    private static String capitalizeFirst(String name) {
        if (name.isEmpty()) return name;
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }
}
