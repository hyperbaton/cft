package com.hyperbaton.cft.structure.home;

public enum HomeDetectionReasons {
    NOT_A_DOOR("Not a door"),
    ALREADY_REGISTERED("House already registered"),
    INVALID_FLOOR("Invalid floor"),
    INVALID_WALLS("Invalid walls"),
    TOO_MANY_DOORS("Too many doors"),
    INVALID_INTERIOR("Invalid interior"),
    INVALID_ROOF("Invalid roof"),
    NO_CLOSURE("There is a gap in the house"),
    NO_CONTAINER("No container present"),
    HOUSE_TOO_LARGE("House too large"),
    HOUSE_DETECTED("House found");

    private final String message;

    HomeDetectionReasons(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

