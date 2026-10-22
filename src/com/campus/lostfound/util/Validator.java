package com.campus.lostfound.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

import com.campus.lostfound.exception.InvalidInputException;

/**
 * Central place for all input validation and date formatting.
 */
public final class Validator {

    public static final String DATE_PATTERN = "dd-MM-yyyy";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static final int MAX_LENGTH = 150;
    private static final Pattern PHONE = Pattern.compile("\\d{10}");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern STUDENT_ID = Pattern.compile("[A-Za-z0-9]{4,15}");

    private Validator() {
        // utility class - no instances
    }

    /** Returns the trimmed text, or throws if it is empty or too long. */
    public static String requireNonBlank(String value, String fieldName) throws InvalidInputException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(fieldName + " cannot be empty.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidInputException(fieldName + " is too long (max " + MAX_LENGTH + " characters).");
        }
        return trimmed;
    }

    public static String validatePhone(String value) throws InvalidInputException {
        String phone = requireNonBlank(value, "Phone number");
        if (!PHONE.matcher(phone).matches()) {
            throw new InvalidInputException("Phone number must be exactly 10 digits.");
        }
        return phone;
    }

    public static String validateEmail(String value) throws InvalidInputException {
        String email = requireNonBlank(value, "E-mail");
        if (!EMAIL.matcher(email).matches()) {
            throw new InvalidInputException("'" + email + "' is not a valid e-mail address.");
        }
        return email;
    }

    public static String validateStudentId(String value) throws InvalidInputException {
        String id = requireNonBlank(value, "Student ID");
        if (!STUDENT_ID.matcher(id).matches()) {
            throw new InvalidInputException("Student ID must be 4-15 letters/digits.");
        }
        return id.toUpperCase();
    }

    /** Parses dd-MM-yyyy and rejects dates in the future. */
    public static LocalDate parseDate(String value) throws InvalidInputException {
        String text = requireNonBlank(value, "Date");
        LocalDate date;
        try {
            date = LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Invalid date '" + text + "'. Use " + DATE_PATTERN
                    + " (for example 18-10-2026).");
        }
        return requireNotFuture(date);
    }

    public static LocalDate requireNotFuture(LocalDate date) throws InvalidInputException {
        if (date == null) {
            throw new InvalidInputException("Date cannot be empty.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidInputException("Date cannot be in the future.");
        }
        return date;
    }

    public static String formatDate(LocalDate date) {
        return DATE_FORMAT.format(date);
    }
}
