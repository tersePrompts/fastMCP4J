package com.ultrathink.fastmcp.progress;

/**
 * Progress reporter for long-running operations.
 * Sends progress notifications to MCP clients.
 *
 * @version 0.2.0
 * @status NOT_IMPLEMENTED
 */
public interface ProgressReporter {

    /**
     * Report progress with percentage
     * @param percentage completion percentage (0-100)
     */
    void report(int percentage);

    /**
     * Report progress with percentage and message
     * @param percentage completion percentage (0-100)
     * @param message progress message
     */
    void report(int percentage, String message);

    /**
     * Report progress with total and current count
     * @param current current item count
     * @param total total item count
     * @param message progress message
     */
    void report(long current, long total, String message);

    /**
     * Mark operation as complete
     */
    void complete();

    /**
     * Mark operation as failed
     * @param error error message
     */
    void fail(String error);
}
