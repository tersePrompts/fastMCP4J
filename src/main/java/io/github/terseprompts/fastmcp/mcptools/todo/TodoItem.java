package io.github.terseprompts.fastmcp.mcptools.todo;

import java.time.Instant;

/**
 * Todo item with metadata.
 */
public record TodoItem(
    String id,
    String task,
    TodoStatus status,
    Instant createdAt,
    Instant updatedAt,
    TodoPriority priority  // NEW: Priority field for sorting
) {
    public TodoItem {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID cannot be null or blank");
        }
        if (task == null || task.isBlank()) {
            throw new IllegalArgumentException("Task cannot be null or blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }
    }
}