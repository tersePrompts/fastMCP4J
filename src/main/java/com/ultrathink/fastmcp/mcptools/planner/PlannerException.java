package com.ultrathink.fastmcp.mcptools.planner;

/**
 * Exception thrown for planner-related errors.
 */
public class PlannerException extends RuntimeException {

    public PlannerException(String message) {
        super(message);
    }

    public PlannerException(String message, Throwable cause) {
        super(message, cause);
    }
}
