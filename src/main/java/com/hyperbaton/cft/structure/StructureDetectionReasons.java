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
    STRUCTURE_DETECTED("Structure detected successfully");

    private final String message;

    StructureDetectionReasons(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
