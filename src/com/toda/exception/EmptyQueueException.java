// src/com/toda/exception/EmptyQueueException.java
package com.toda.exception;

/** Thrown when an operation needs a tricycle in the queue but the queue is empty. */
public class EmptyQueueException extends TerminalException {
    private static final long serialVersionUID = 1L;

    public EmptyQueueException(String message) {
        super(message);
    }

    @Override
    public int getHttpStatus() {
        return 409;
    }

    @Override
    public String getErrorCode() {
        return "EMPTY_QUEUE";
    }
}
