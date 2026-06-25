package com.hyperbaton.cft.structure;

public enum MonumentBlockGroup {
    BODY("body");

    private final String key;

    MonumentBlockGroup(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
