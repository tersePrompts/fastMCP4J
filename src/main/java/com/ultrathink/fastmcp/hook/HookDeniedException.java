package com.ultrathink.fastmcp.hook;

/**
 * Thrown when a hook denies tool execution.
 */
public class HookDeniedException extends RuntimeException {

    private final String toolName;

    public HookDeniedException(String message) {
        super(message);
        this.toolName = extractToolName(message);
    }

    public HookDeniedException(String toolName, String message) {
        super(message);
        this.toolName = toolName;
    }

    private static String extractToolName(String message) {
        // Try to extract tool name from message like "Tool 'xxx' denied by hook..."
        int start = message.indexOf('\'');
        int end = message.indexOf('\'', start + 1);
        if (start > 0 && end > start) {
            return message.substring(start + 1, end);
        }
        return "unknown";
    }

    public String getToolName() {
        return toolName;
    }
}
