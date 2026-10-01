package com.hyperbaton.cft.structure;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Reasons a structure detection attempt can fail (or succeed).
 *
 * The ORDER of the entries matters: when several structure types share the same key
 * block and all of them fail, the player is shown only the failure of the type that
 * got "closest" to being detected — and closeness is measured by the ordinal of its
 * reason in this enum. Reasons appearing later are considered further along the
 * detection process (i.e. closer to success) than earlier ones.
 *
 * When adding a new reason, insert it at a position that reflects how deep into the
 * detection process the failure happens, rather than just appending it at the end.
 */
public enum StructureDetectionReasons {
    // --- Preconditions: detection could not even start ---
    NOT_A_KEY_BLOCK,
    ALREADY_REGISTERED,
    OVERLAPPING_STRUCTURE,

    // --- Base shape: floor, border or first layer not found or invalid ---
    NO_FLOOR,
    FLOOR_TOO_BIG,
    INVALID_BORDER,
    BORDER_NOT_CLOSED,
    INVALID_MONUMENT_LAYER,
    INVALID_FLOOR,
    INVALID_GROUND_PERIMETER,
    SURFACE_TOO_BIG,
    INVALID_SURFACE,

    // --- Body: walls, interior, roof and enclosure ---
    INVALID_WALLS,
    INVALID_INTERIOR,
    INVALID_ROOF,
    CEILING_NOT_FLAT,
    NO_CLOSURE,
    NO_SKY_ACCESS,

    // --- Aggregate checks: the body exists but has wrong proportions ---
    MONUMENT_TOO_SHORT,
    MONUMENT_TOO_TALL,
    NOT_ENOUGH_STOREYS,
    LAYERS_NOT_IDENTICAL,

    // --- Final checks: the structure is essentially complete ---
    MISSING_REQUIRED_STRUCTURES,
    NO_CONTAINER,
    TOO_DARK,
    NOT_ENOUGH_ANIMALS,
    STRUCTURE_TOO_LARGE,

    STRUCTURE_DETECTED,
    STRUCTURE_CONFIRMED;

    /** The reason, for the player: each one has a lang entry {@code detection.cft.reason.<name>}. */
    public Component getMessage() {
        return Component.translatable("detection.cft.reason." + name().toLowerCase(Locale.ROOT));
    }
}
