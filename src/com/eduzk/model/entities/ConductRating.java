package com.eduzk.model.entities;

public enum ConductRating {
    EXCELLENT("Xuất sắc"),
    GOOD("Tốt"),
    FAIR("Khá"),
    AVERAGE("Trung bình"),
    WEAK("Yếu");

    private final String displayName;

    ConductRating(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static ConductRating fromString(String text) {
        for (ConductRating b : ConductRating.values()) {
            if (b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}