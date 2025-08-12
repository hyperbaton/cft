package com.hyperbaton.cft.entity.custom;

import java.util.*;

public class XoonglinNameGenerator {
    private static final Random RANDOM = new Random();
    
    // Training data - a collection of Xoonglin-style names to learn patterns from
    private static final List<String> TRAINING_NAMES = List.of(
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
    
    // Markov chain order (how many previous characters to consider)
    private static final int CHAIN_ORDER = 2;
    
    // Character transition probabilities
    private static final Map<String, List<Character>> TRANSITIONS = new HashMap<>();
    
    // Initialize the Markov chain
    static {
        buildMarkovChain();
    }
    
    private static void buildMarkovChain() {
        for (String name : TRAINING_NAMES) {
            String processedName = "^" + name.toLowerCase() + "$"; // Add start/end markers
            
            // Build transitions for each n-gram
            for (int i = 0; i <= processedName.length() - CHAIN_ORDER - 1; i++) {
                String state = processedName.substring(i, i + CHAIN_ORDER);
                char nextChar = processedName.charAt(i + CHAIN_ORDER);
                
                TRANSITIONS.computeIfAbsent(state, k -> new ArrayList<>()).add(nextChar);
            }
        }
    }
    
    public static String generateName() {
        StringBuilder name = new StringBuilder();
        String currentState = "^".repeat(CHAIN_ORDER); // Start state
        
        int maxAttempts = 50; // Prevent infinite loops
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            List<Character> possibleNext = TRANSITIONS.get(currentState);
            
            if (possibleNext == null || possibleNext.isEmpty()) {
                // If we hit a dead end, try a different approach
                if (name.length() < 3) {
                    // Start over with a random valid starting state
                    currentState = getRandomStartState();
                    continue;
                } else {
                    // End the name here
                    break;
                }
            }
            
            char nextChar = possibleNext.get(RANDOM.nextInt(possibleNext.size()));
            
            if (nextChar == '$') {
                // End of name marker
                break;
            }
            
            if (nextChar != '^') {
                name.append(nextChar);
            }
            
            // Update state (sliding window)
            currentState = currentState.substring(1) + nextChar;
            attempts++;
        }
        
        String result = name.toString();
        
        // Ensure minimum length and capitalize
        if (result.length() < 3) {
            return generateFallbackName();
        }
        
        return capitalizeFirst(result);
    }
    
    private static String getRandomStartState() {
        List<String> startStates = new ArrayList<>();
        for (String state : TRANSITIONS.keySet()) {
            if (state.startsWith("^")) {
                startStates.add(state);
            }
        }
        return startStates.isEmpty() ? "^".repeat(CHAIN_ORDER) : 
               startStates.get(RANDOM.nextInt(startStates.size()));
    }
    
    private static String generateFallbackName() {
        // Fallback to the original method if Markov chain fails
        List<String> prefixes = List.of("Xoo", "Zyn", "Glo", "Plo", "Fle", "Dri");
        List<String> suffixes = List.of("lin", "gla", "bo", "qui", "ni", "tro");
        
        String prefix = prefixes.get(RANDOM.nextInt(prefixes.size()));
        String suffix = suffixes.get(RANDOM.nextInt(suffixes.size()));
        return prefix + suffix;
    }
    
    private static String capitalizeFirst(String name) {
        if (name.isEmpty()) return name;
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}