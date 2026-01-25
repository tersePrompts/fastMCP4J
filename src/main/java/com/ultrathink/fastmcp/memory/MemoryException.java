package com.ultrathink.fastmcp.memory;

/**
 * Exception thrown for memory-related errors.
 */
public class MemoryException extends RuntimeException {

    public MemoryException(String message) {
        super(message);
    }

    public MemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
