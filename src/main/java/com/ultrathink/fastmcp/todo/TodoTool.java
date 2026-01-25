package com.ultrathink.fastmcp.todo;

import com.ultrathink.fastmcp.annotations.McpParam;
import com.ultrathink.fastmcp.annotations.McpTool;

import java.util.ArrayList;
import java.util.List;

/**
 * Todo tool using FastMCP4J annotations.
 * <p>
 * Provides task management capabilities:
 * - Add new todos
 * - List todos (optionally filtered by status)
 * - Update todo status
 * - Delete todos
 * - Clear completed todos
 * <p>
 * Usage: Add {@code @McpTodo} to server class to enable.
 *
 * @see com.ultrathink.fastmcp.annotations.McpTodo
 */
public class TodoTool {

    private final TodoStore store;

    public TodoTool(TodoStore store) {
        this.store = store;
    }

    /**
     * Add a new todo item.
     */
    @McpTool(description = "Add a new todo item to the list")
    public String add(
        @McpParam(
            description = "The task description to add",
            examples = {"Implement authentication", "Write tests for user module", "Deploy to production"},
            constraints = "Cannot be empty or blank"
        )
        String task
    ) {
        String id = store.add(task);
        return String.format("Added todo (ID: %s): %s", id, task);
    }

    /**
     * List all todos or filter by status.
     */
    @McpTool(description = "List all todos, optionally filtered by status")
    public String list(
        @McpParam(
            description = "Filter by status: 'pending', 'in_progress', or 'completed'. Leave empty for all.",
            examples = {"pending", "completed", "in_progress"},
            required = false
        )
        String status
    ) {
        TodoStatus filter = parseStatus(status);
        List<TodoItem> todos = store.list(filter);

        if (todos.isEmpty()) {
            return String.format("No %stodos found.", filter != null ? filter.toString().toLowerCase() + " " : "");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(formatHeader(filter));
        
        for (TodoItem todo : todos) {
            sb.append(formatTodo(todo));
        }

        sb.append(formatSummary());

        return sb.toString();
    }

    /**
     * Update a todo item's status.
     */
    @McpTool(description = "Update the status of an existing todo item")
    public String update(
        @McpParam(
            description = "The ID of the todo to update",
            examples = {"abc12345", "def67890"}
        )
        String id,

        @McpParam(
            description = "The new status: 'pending', 'in_progress', or 'completed'",
            examples = {"completed", "in_progress", "pending"},
            constraints = "Must be a valid status value"
        )
        String status
    ) {
        TodoStatus newStatus = parseStatus(status);
        if (newStatus == null) {
            throw new TodoException("Invalid status: " + status + ". Use: pending, in_progress, completed");
        }

        TodoItem existing = store.get(id);
        if (existing == null) {
            throw new TodoException("Todo not found with ID: " + id);
        }

        store.updateStatus(id, newStatus);
        return String.format("Updated todo %s: %s -> %s", id, existing.status().toString().toLowerCase(), newStatus.toString().toLowerCase());
    }

    /**
     * Delete a todo item.
     */
    @McpTool(description = "Delete a todo item by ID")
    public String delete(
        @McpParam(
            description = "The ID of the todo to delete",
            examples = {"abc12345", "def67890"}
        )
        String id
    ) {
        TodoItem existing = store.get(id);
        if (existing == null) {
            throw new TodoException("Todo not found with ID: " + id);
        }

        store.delete(id);
        return String.format("Deleted todo (ID: %s): %s", id, existing.task());
    }

    /**
     * Clear all completed todos.
     */
    @McpTool(description = "Delete all todos that are marked as completed")
    public String clearCompleted() {
        int completedCount = store.count(TodoStatus.COMPLETED);
        
        if (completedCount == 0) {
            return "No completed todos to clear.";
        }

        store.clearCompleted();
        return String.format("Cleared %d completed todo(s).", completedCount);
    }

    private TodoStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.toLowerCase()) {
            case "pending" -> TodoStatus.PENDING;
            case "in_progress" -> TodoStatus.IN_PROGRESS;
            case "completed", "complete", "done" -> TodoStatus.COMPLETED;
            default -> throw new TodoException("Invalid status: " + status + ". Use: pending, in_progress, completed");
        };
    }

    private String formatHeader(TodoStatus filter) {
        int total = store.count(null);
        int pending = store.count(TodoStatus.PENDING);
        int inProgress = store.count(TodoStatus.IN_PROGRESS);
        int completed = store.count(TodoStatus.COMPLETED);

        StringBuilder sb = new StringBuilder();
        sb.append("📋 Todo List\n");
        sb.append("=".repeat(40)).append("\n");
        if (filter != null) {
            sb.append("Filtered by: ").append(filter.toString().toLowerCase()).append("\n");
        }
        sb.append(String.format("Total: %d | Pending: %d | In Progress: %d | Completed: %d\n",
            total, pending, inProgress, completed));
        sb.append("=".repeat(40)).append("\n\n");
        return sb.toString();
    }

    private String formatTodo(TodoItem todo) {
        String statusIcon = switch (todo.status()) {
            case PENDING -> "⏳";
            case IN_PROGRESS -> "🔄";
            case COMPLETED -> "✅";
        };

        return String.format("%s [%s] %s\n",
            statusIcon,
            todo.id(),
            todo.task()
        );
    }

    private String formatSummary() {
        int pending = store.count(TodoStatus.PENDING);
        int inProgress = store.count(TodoStatus.IN_PROGRESS);
        int completed = store.count(TodoStatus.COMPLETED);

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("-".repeat(40)).append("\n");
        sb.append("Next: Start a pending task or update status\n");
        sb.append(String.format("Summary: %d pending, %d in progress, %d completed",
            pending, inProgress, completed));
        return sb.toString();
    }
}
