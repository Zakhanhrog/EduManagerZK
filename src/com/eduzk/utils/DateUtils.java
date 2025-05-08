package com.eduzk.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        try {
            return date.format(DATE_FORMATTER);
        } catch (Exception e) {
            return date.toString();
        }
    }

    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing date string: " + dateString + " - " + e.getMessage());
            return null;
        }
    }

    public static String formatTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        try {
            return time.format(TIME_FORMATTER);
        } catch (Exception e) {
            return time.toString();
        }
    }

    public static LocalTime parseTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(timeString.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing time string: " + timeString + " - " + e.getMessage());
            return null;
        }
    }

    public static boolean doTimesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        if (end1.isBefore(start1) || end2.isBefore(start2)) {
            return false;
        }

        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}