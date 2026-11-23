// src/com/toda/exception/NotFoundException.java
package com.toda.exception;

/** Thrown when a driver, destination or queued tricycle cannot be found. */
public class NotFoundException extends TerminalException {
    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }

    @Override
    public int getHttpStatus() {
        return 404;
    }

    @Override
    public String getErrorCode() {
        return "NOT_FOUND";
    }
}
