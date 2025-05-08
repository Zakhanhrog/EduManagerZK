package com.eduzk.model.entities;

public enum ArtStatus {
    PASSED("Đạt"),
    FAILED("Không đạt");

    private final String displayName;
    ArtStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
    public static ArtStatus fromString(String text) {
        for (ArtStatus b : ArtStatus.values()) {
            if (b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}