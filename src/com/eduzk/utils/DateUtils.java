package com.eduzk.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtils {

    // Define common date and time formats
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Format LocalDate to String
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        try {
            return date.format(DATE_FORMATTER);
        } catch (Exception e) {
            // Log or handle formatting error if needed
            return date.toString(); // Fallback to default format
        }
    }

    // Parse String to LocalDate
    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // Log or handle parsing error if needed
            System.err.println("Error parsing date string: " + dateString + " - " + e.getMessage());
            return null; // Return null or throw custom exception
        }
    }

    // Format LocalTime to String
    public static String formatTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        try {
            return time.format(TIME_FORMATTER);
        } catch (Exception e) {
            // Log or handle formatting error
            return time.toString(); // Fallback
        }
    }

    // Parse String to LocalTime
    public static LocalTime parseTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(timeString.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            // Log or handle parsing error
            System.err.println("Error parsing time string: " + timeString + " - " + e.getMessage());
            return null; // Return null or throw custom exception
        }
    }

    // Helper method to check if two time slots overlap on the same day
    public static boolean doTimesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            // Cannot determine overlap if any time is missing
            return false;
        }
        // Ensure end times are not before start times within the same slot
        if (end1.isBefore(start1) || end2.isBefore(start2)) {
            // Consider logging this as an invalid input state
            return false; // Or throw an exception for invalid interval
        }

        // Check for overlap: (StartA < EndB) and (EndA > StartB)
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}