package com.ultrathink.fastmcp.hook;

import java.util.Map;

/**
 * Interface for hook executors.
 * <p>
 * Implementations can be Java-based or loaded from external configurations.
 */
public interface HookExecutor {

    /**
     * Execute a PRE_TOOL_USE or POST_TOOL_USE hook.
     *
     * @param toolName The tool being called
     * @param arguments The tool arguments (may be modified by PRE hooks)
     * @return The hook result
     */
    default HookResult execute(String toolName, Map<String, Object> arguments) {
        return HookResult.allow();
    }

    /**
     * Execute a POST_TOOL_USE hook with result.
     *
     * @param toolName The tool that was called
     * @param arguments The tool arguments
     * @param result The tool result
     * @return The hook result (may modify result)
     */
    default HookResult execute(String toolName, Map<String, Object> arguments, Object result) {
        return HookResult.allow();
    }

    /**
     * Execute a session lifecycle hook.
     *
     * @param sessionId The session ID
     * @param context Session context (tenantId, userId, etc.)
     */
    default void executeSessionHook(String sessionId, Map<String, Object> context) {}
}
