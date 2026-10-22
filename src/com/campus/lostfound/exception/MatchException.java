package com.campus.lostfound.exception;

/**
 * Checked exception thrown when a match or claim operation is not allowed
 * (for example: item already matched, wrong student ID while claiming).
 */
public class MatchException extends Exception {

    private static final long serialVersionUID = 1L;

    public MatchException(String message) {
        super(message);
    }
}
