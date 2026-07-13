package com.hyperbaton.cft.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * The word lists and sentence/title templates a WriterJob draws from to generate book
 * text. Datapack-configurable so different writer jobs (e.g. different "cultures" in a
 * modpack) can produce recognizably different books, including in different languages —
 * this mod has no opinion on the actual words, only on how templates are filled.
 *
 * Templates are plain strings with {@code %category%} placeholders (e.g. {@code %noun%},
 * {@code %adjective%}, {@code %verb%}, {@code %place%}), each replaced with a random word
 * from the matching list. Unknown placeholders are left as-is rather than failing, so a
 * typo in a modpack's template shows up visibly instead of crashing generation.
 */
public record TextBank(
        List<String> nouns,
        List<String> adjectives,
        List<String> verbs,
        List<String> places,
        List<String> titleTemplates,
        List<String> sentenceTemplates
) {
    public static final Codec<TextBank> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.listOf().fieldOf("nouns").forGetter(TextBank::nouns),
            Codec.STRING.listOf().fieldOf("adjectives").forGetter(TextBank::adjectives),
            Codec.STRING.listOf().fieldOf("verbs").forGetter(TextBank::verbs),
            Codec.STRING.listOf().fieldOf("places").forGetter(TextBank::places),
            Codec.STRING.listOf().fieldOf("title_templates").forGetter(TextBank::titleTemplates),
            Codec.STRING.listOf().fieldOf("sentence_templates").forGetter(TextBank::sentenceTemplates)
    ).apply(inst, TextBank::new));

    /** The word bank used when a WriterJob doesn't configure its own. */
    public static final TextBank DEFAULT = new TextBank(
            List.of("miller", "lantern", "hollow", "raven", "ferry", "orchard", "well", "cairn",
                    "harbor", "meadow", "chronicle", "shepherd", "quarry", "ember", "loom",
                    "wanderer", "grove", "tide", "forge", "wren"),
            List.of("quiet", "restless", "hollow", "distant", "faded", "stubborn", "gilded",
                    "weary", "unspoken", "crooked", "silver", "brittle", "patient", "wandering"),
            List.of("spoke of", "waited for", "forgot", "carried", "outlived", "circled",
                    "mistook", "remembered", "abandoned", "followed"),
            List.of("the valley", "the old road", "the harbor town", "the north fields",
                    "the empty house", "the market square", "the long winter", "the last village"),
            List.of(
                    "The %noun% of the %adjective% %place%",
                    "%noun% and the %adjective% %noun%",
                    "A %adjective% %noun%",
                    "The %adjective% %noun%"
            ),
            List.of(
                    "The %adjective% %noun% %verb% the %adjective% %noun% in %place%.",
                    "%noun% was never %adjective% in %place%.",
                    "No one %verb% the %adjective% %noun% again.",
                    "In %place%, the %adjective% %noun% stayed %adjective%."
            )
    );

    /** A random word from the named category, or null if the category is unknown/empty. */
    public String pickWord(String category, RandomSource random) {
        List<String> bank = switch (category) {
            case "noun" -> nouns;
            case "adjective" -> adjectives;
            case "verb" -> verbs;
            case "place" -> places;
            default -> null;
        };
        if (bank == null || bank.isEmpty()) return null;
        return bank.get(random.nextInt(bank.size()));
    }
}
