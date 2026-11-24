// src/com/toda/exception/TerminalException.java
package com.toda.exception;

/**
 * Root of every domain exception in the terminal. Each subclass declares the HTTP status it
 * maps to, so the controller can translate failures polymorphically (no instanceof chains).
 */
public abstract class TerminalException extends Exception {
    private static final long serialVersionUID = 1L;

    protected TerminalException(String message) {
        super(message);
    }

    /** HTTP status code the controller should answer with. */
    public abstract int getHttpStatus();

    /** Short machine-readable error code for the JSON payload. */
    public abstract String getErrorCode();
}
