// src/com/toda/exception/CapacityExceededException.java
package com.toda.exception;

/** Thrown when boarding would put more passengers in a tricycle than it can seat. */
public class CapacityExceededException extends TerminalException {
    private static final long serialVersionUID = 1L;

    public CapacityExceededException(String message) {
        super(message);
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }

    @Override
    public String getErrorCode() {
        return "CAPACITY_EXCEEDED";
    }
}
