// src/com/toda/exception/InvalidInputException.java
package com.toda.exception;

/** Thrown when a value fails validation (negative fare, blank body number, bad status...). */
public class InvalidInputException extends TerminalException {
    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }

    @Override
    public int getHttpStatus() {
        return 400;
    }

    @Override
    public String getErrorCode() {
        return "INVALID_INPUT";
    }
}
