package com.eduzk.model.entities;

public enum Role {
    ADMIN("Administrator"),
    TEACHER("Teacher"),
    STUDENT("Student");
    // Add STUDENT("Student"), PARENT("Parent") if needed later

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName; // For display in UI components like ComboBoxes
    }
}