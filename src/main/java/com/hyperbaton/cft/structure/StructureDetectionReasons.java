package com.hyperbaton.cft.structure;

public enum StructureDetectionReasons {
    NOT_A_KEY_BLOCK("Not a key block for any structure type"),
    ALREADY_REGISTERED("Structure already registered at this position"),
    NO_FLOOR("No valid floor found"),
    FLOOR_TOO_BIG("Floor exceeds maximum size"),
    INVALID_FLOOR("Floor blocks do not meet requirements"),
    INVALID_WALLS("Wall blocks do not meet requirements"),
    INVALID_INTERIOR("Interior blocks do not meet requirements"),
    INVALID_ROOF("Roof blocks do not meet requirements"),
    NO_CLOSURE("There is a gap in the structure"),
    NO_CONTAINER("No container found (required by structure type)"),
    STRUCTURE_TOO_LARGE("Structure exceeds maximum size"),
    INVALID_BORDER("Border does not form a valid perimeter"),
    BORDER_NOT_CLOSED("Border is not a closed one-block-wide loop"),
    INVALID_GROUND_PERIMETER("Ground perimeter blocks do not meet requirements"),
    INVALID_SURFACE("Surface blocks do not meet requirements"),
    SURFACE_TOO_BIG("Surface exceeds maximum size"),
    NO_SKY_ACCESS("Surface blocks must be open to the sky"),
    INVALID_MONUMENT_LAYER("Monument layer blocks do not meet requirements"),
    MONUMENT_TOO_SHORT("Monument does not reach the minimum height"),
    MONUMENT_TOO_TALL("Monument exceeds the maximum height"),
    LAYERS_NOT_IDENTICAL("Layers required to be identical differ in shape or block types"),
    STRUCTURE_DETECTED("Structure detected successfully");

    private final String message;

    StructureDetectionReasons(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
