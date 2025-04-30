package com.eduzk.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

public class ValidationUtils {

    // Basic non-empty string validation
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // Email validation (basic pattern)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    public static boolean isValidEmail(String email) {
        return isNotEmpty(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // Phone number validation (allows digits, spaces, hyphens, parentheses, +) - adjust as needed
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9\\s\\-\\(\\)]{7,20}$" // Example: Allows +1 (123) 456-7890 or 0987654321
    );

    public static boolean isValidPhoneNumber(String phone) {
        return isNotEmpty(phone) && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    // Positive integer validation
    public static boolean isPositiveInteger(String value) {
        if (!isNotEmpty(value)) {
            return false;
        }
        try {
            int intValue = Integer.parseInt(value.trim());
            return intValue > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Non-negative integer validation (includes zero)
    public static boolean isNonNegativeInteger(String value) {
        if (!isNotEmpty(value)) {
            // Consider if an empty string should be valid (e.g., default to 0)
            // For strict validation, empty is false.
            return false;
        }
        try {
            int intValue = Integer.parseInt(value.trim());
            return intValue >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Username validation (example: alphanumeric, underscore, 3-20 chars)
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{3,20}$"
    );

    public static boolean isValidUsername(String username) {
        return isNotEmpty(username) && USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    // Password validation (example: at least 6 characters) - Make this stronger!
    public static boolean isValidPassword(String password) {
        // IMPORTANT: This is a very weak example. Use stronger criteria in production.
        // e.g., length, uppercase, lowercase, number, special character requirements.
        return password != null && password.length() >= 6;
    }

    // Date validation (checks if not null)
    public static boolean isValidDate(LocalDate date) {
        return date != null;
    }

    // Time validation (checks if not null)
    public static boolean isValidTime(LocalTime time) {
        return time != null;
    }

    // Time range validation (checks if end time is after start time)
    public static boolean isValidTimeRange(LocalTime startTime, LocalTime endTime) {
        // Allow start time equals end time for simplicity, adjust if needed
        return isValidTime(startTime) && isValidTime(endTime) && !endTime.isBefore(startTime);
    }

}