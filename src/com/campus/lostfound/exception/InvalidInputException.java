package com.campus.lostfound.exception;

/**
 * Checked exception thrown when user input (name, date, phone, e-mail, ...)
 * fails validation.
 */
public class InvalidInputException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
