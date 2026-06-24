package com.hyperbaton.cft.structure;

public enum EnclosedBuildingBlockGroup {
    FLOOR("floor"),
    WALL("wall"),
    INTERIOR("interior"),
    ROOF("roof");

    private final String key;

    EnclosedBuildingBlockGroup(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
