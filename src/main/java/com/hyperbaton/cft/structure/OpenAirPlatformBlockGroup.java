package com.hyperbaton.cft.structure;

public enum OpenAirPlatformBlockGroup {
    BORDER("border"),
    GROUND_PERIMETER("ground_perimeter"),
    SURFACE("surface");

    private final String key;

    OpenAirPlatformBlockGroup(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
