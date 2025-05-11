package com.eduzk.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ValidationUtils {

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    public static boolean isValidEmail(String email) {
        return isNotEmpty(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9\\s\\-\\(\\)]{7,20}$"
    );

    public static boolean isValidPhoneNumber(String phone) {
        return isNotEmpty(phone) && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{3,20}$"
    );

    public static boolean isValidUsername(String username) {
        return isNotEmpty(username) && USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`].*");

    public static boolean isValidStrongPassword(String password) {
        if (password == null) {
            return false;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            return false;
        }
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            return false;
        }
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            return false;
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            return false;
        }
        return true;
    }

    public static List<String> getPasswordValidationErrors(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            errors.add("Mật khẩu không được để trống.");
            return errors; // Trả về ngay nếu rỗng
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.add("- Ít nhất " + MIN_PASSWORD_LENGTH + " ký tự.");
        }
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            errors.add("- Ít nhất một chữ hoa (A-Z).");
        }
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            errors.add("- Ít nhất một chữ thường (a-z).");
        }
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            errors.add("- Ít nhất một chữ số (0-9).");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            errors.add("- Ít nhất một ký tự đặc biệt (ví dụ: !@#$%).");
        }
        return errors;
    }

    public static boolean isValidDate(LocalDate date) {
        return date != null;
    }

    public static boolean isValidTime(LocalTime time) {
        return time != null;
    }

    public static boolean isValidTimeRange(LocalTime startTime, LocalTime endTime) {
        return isValidTime(startTime) && isValidTime(endTime) && !endTime.isBefore(startTime);
    }

}