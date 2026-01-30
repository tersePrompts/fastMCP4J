package com.ultrathink.fastmcp.agent.spi;

import com.ultrathink.fastmcp.context.Context;

/**
 * SPI for initializing sessions.
 * Implementations can perform session-specific setup.
 */
public interface SessionInitializer {

    /**
     * Initialize a new session
     * @param sessionId The MCP session ID
     * @param context The current context
     */
    void initialize(String sessionId, Context context);

    /**
     * Priority for initializer ordering (lower = earlier)
     */
    default int priority() {
        return 100;
    }

    /**
     * Whether this initializer is enabled
     */
    default boolean isEnabled() {
        return true;
    }
}
