package com.ultrathink.fastmcp.agent.state;

/**
 * Exception thrown when a state operation fails.
 */
public class StateException extends RuntimeException {

    private final String path;

    public StateException(String message) {
        super(message);
        this.path = null;
    }

    public StateException(String message, Throwable cause) {
        super(message, cause);
        this.path = null;
    }

    public StateException(String path, String message) {
        super(message);
        this.path = path;
    }

    public StateException(String path, String message, Throwable cause) {
        super(message, cause);
        this.path = path;
    }

    /**
     * Get the path associated with this exception, if available.
     */
    public String getPath() {
        return path;
    }
}
