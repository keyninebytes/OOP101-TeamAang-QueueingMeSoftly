// src/com/toda/exception/DuplicateEntryException.java
package com.toda.exception;

/** Thrown when a unique value (body number, license, destination) already exists. */
public class DuplicateEntryException extends TerminalException {
    private static final long serialVersionUID = 1L;

    public DuplicateEntryException(String message) {
        super(message);
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }

    @Override
    public String getErrorCode() {
        return "DUPLICATE_ENTRY";
    }
}
