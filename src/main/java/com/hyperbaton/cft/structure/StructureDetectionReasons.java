package com.hyperbaton.cft.structure;

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
    NOT_A_KEY_BLOCK("Not a key block for any structure type"),
    ALREADY_REGISTERED("Structure already registered at this position"),
    OVERLAPPING_STRUCTURE("Structure overlaps an already registered structure"),

    // --- Base shape: floor, border or first layer not found or invalid ---
    NO_FLOOR("No valid floor found"),
    FLOOR_TOO_BIG("Floor exceeds maximum size"),
    INVALID_BORDER("Border does not form a valid perimeter"),
    BORDER_NOT_CLOSED("Border is not a closed one-block-wide loop"),
    INVALID_MONUMENT_LAYER("Monument layer blocks do not meet requirements"),
    INVALID_FLOOR("Floor blocks do not meet requirements"),
    INVALID_GROUND_PERIMETER("Ground perimeter blocks do not meet requirements"),
    SURFACE_TOO_BIG("Surface exceeds maximum size"),
    INVALID_SURFACE("Surface blocks do not meet requirements"),

    // --- Body: walls, interior, roof and enclosure ---
    INVALID_WALLS("Wall blocks do not meet requirements"),
    INVALID_INTERIOR("Interior blocks do not meet requirements"),
    INVALID_ROOF("Roof blocks do not meet requirements"),
    CEILING_NOT_FLAT("Storey ceiling must be flat to support another storey"),
    NO_CLOSURE("There is a gap in the structure"),
    NO_SKY_ACCESS("Surface blocks must be open to the sky"),

    // --- Aggregate checks: the body exists but has wrong proportions ---
    MONUMENT_TOO_SHORT("Monument does not reach the minimum height"),
    MONUMENT_TOO_TALL("Monument exceeds the maximum height"),
    NOT_ENOUGH_STOREYS("Building does not reach the minimum number of storeys"),
    LAYERS_NOT_IDENTICAL("Layers required to be identical differ in shape or block types"),

    // --- Final checks: the structure is essentially complete ---
    MISSING_REQUIRED_STRUCTURES("Required nearby structures are missing or too far"),
    NO_CONTAINER("No container found (required by structure type)"),
    STRUCTURE_TOO_LARGE("Structure exceeds maximum size"),

    STRUCTURE_DETECTED("Structure detected successfully");

    private final String message;

    StructureDetectionReasons(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
