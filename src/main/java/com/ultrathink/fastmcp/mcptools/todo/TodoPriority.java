package com.ultrathink.fastmcp.mcptools.todo;

/**
 * Priority levels for todo items.
 * Used for sorting and display importance.
 */
public enum TodoPriority {
    LOW(0, "Low"),
    MEDIUM(1, "Medium"),
    HIGH(2, "High"),
    CRITICAL(3, "Critical");

    private final int value;
    private final String displayName;

    TodoPriority(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public int getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse a string to TodoPriority.
     */
    public static TodoPriority fromString(String priority) {
        if (priority == null || priority.isBlank()) {
            return MEDIUM; // Default
        }
        return switch (priority.toLowerCase()) {
            case "low" -> LOW;
            case "medium" -> MEDIUM;
            case "high" -> HIGH;
            case "critical" -> CRITICAL;
            default -> MEDIUM;
        };
    }
}
