package com.campus.lostfound.exception;

/**
 * Checked exception that wraps any problem while reading or writing data
 * files (IOException, corrupt data lines, ...).
 */
public class StorageException extends Exception {

    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
