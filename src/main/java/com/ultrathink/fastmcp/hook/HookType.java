package com.ultrathink.fastmcp.hook;

/**
 * Types of hooks in the MCP lifecycle.
 * <p>
 * Extends the existing PRE_TOOL_USE and POST_TOOL_USE concepts
 * with session lifecycle hooks.
 */
public enum HookType {
    /**
     * Called before a tool is invoked.
     * Can DENY, MODIFY arguments, or ALLOW execution.
     */
    PRE_TOOL_USE,

    /**
     * Called after a tool is invoked.
     * Can MODIFY the result or observe.
     */
    POST_TOOL_USE,

    /**
     * Called when a new session is bootstrapped.
     * Can initialize session state, validate tenant/user access.
     */
    SESSION_BOOTSTRAP,

    /**
     * Called when a session becomes active.
     * Can set up session-scoped resources.
     */
    SESSION_START,

    /**
     * Called when a session is about to expire.
     * Can persist state, clean up resources.
     */
    SESSION_EXPIRING,

    /**
     * Called when a session ends.
     * Can clean up resources, log metrics.
     */
    SESSION_END
}
