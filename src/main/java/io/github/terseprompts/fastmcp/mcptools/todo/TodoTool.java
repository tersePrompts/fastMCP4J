package io.github.terseprompts.fastmcp.mcptools.todo;

import io.github.terseprompts.fastmcp.annotations.McpParam;
import io.github.terseprompts.fastmcp.annotations.McpTool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Todo tool using FastMCP4J annotations.
 * <p>
 * Provides unified task management through a single tool with multiple modes:
 * - Add new todos
 * - List todos (optionally filtered by status)
 * - Update todo status
 * - Update todo task/description
 * - Delete todos
 * - Clear completed todos
 * <p>
 * Usage: Add {@code @McpTodo} to server class to enable.
 *
 * @see io.github.terseprompts.fastmcp.annotations.McpTodo
 */
public class TodoTool {

    private final TodoStore store;

    public TodoTool(TodoStore store) {
        this.store = store;
    }

    /**
     * Unified todo management tool supporting multiple operations.
     * <p>
     * This single method handles all todo operations based on the mode parameter.
     * Each mode has its own set of required and optional parameters.
     * <p>
     * Mode-specific behaviors:
     * <ul>
     *   <li><b>add</b>: Creates a new todo with description and optional priority</li>
     *   <li><b>list</b>: Displays todos, optionally filtered by status and sorted</li>
     *   <li><b>update</b>: Changes todo status or description</li>
     *   <li><b>delete</b>: Removes a todo permanently</li>
     *   <li><b>clearCompleted</b>: Bulk deletes all completed todos</li>
     * </ul>
     */
    @McpTool(
        description = """
            Unified todo management tool. Supports multiple modes:
            - add: Create new todos
            - list: Display todos (with optional filtering)
            - update: Modify todo status or description
            - delete: Remove todos
            - clearCompleted: Delete all completed todos at once

            Each mode has its own parameters. Provide the mode and its corresponding parameters.
            """
    )
    public String todo(
        @McpParam(
            description = """
                The operation to perform. Each mode has specific parameters:

                **add** - Create a new todo:
                  - task (required): Description of the task
                  - priority (optional): Task priority level (low/medium/high/critical)

                **list** - Display todos:
                  - status (optional): Filter by status (pending/in_progress/completed)
                  - sort (optional): Sort order (status/date/priority/alpha)

                **update** - Modify existing todo:
                  - id (required): Todo identifier
                  - status (optional): New status value
                  - task (optional): New description (if changing text)

                **delete** - Remove a todo:
                  - id (required): Todo identifier to delete

                **clearCompleted** - Bulk delete:
                  - No additional parameters needed
                  - Removes all todos with completed status
                """,
            examples = {"add", "list", "update", "delete", "clearCompleted"},
            constraints = "Must be one of: add, list, update, delete, clearCompleted",
            hints = "The mode determines which other parameters are required. Start with the operation you want to perform."
        )
        String mode,

        // ====== ADD MODE PARAMETERS ======
        @McpParam(
            description = """
                [ADD MODE] The task description. Should be clear, concise, and actionable.

                Examples of good task descriptions:
                - "Implement user authentication flow"
                - "Write unit tests for PaymentService"
                - "Review pull request #123"
                - "Update documentation for API v2"
                """,
            examples = {
                "Implement user authentication",
                "Write tests for PaymentService",
                "Deploy to production environment",
                "Review PR #142: Fix login bug",
                "Update API documentation"
            },
            constraints = "Required for 'add' mode. Cannot be empty or blank. Should be 1-500 characters.",
            hints = "Make tasks specific and actionable. Use verbs to start (Implement, Write, Fix, etc.).",
            required = false
        )
        String task,

        @McpParam(
            description = """
                [ADD MODE] Optional priority level for the task.

                Priority levels:
                - low: Nice to have, can be deferred
                - medium: Standard priority, default
                - high: Important, should be done soon
                - critical: Urgent, blocks other work
                """,
            examples = {"low", "medium", "high", "critical"},
            constraints = "Optional for 'add' mode. Must be one of: low, medium, high, critical. Defaults to 'medium'.",
            hints = "Only set priority for tasks that truly need it. Most tasks should use 'medium' (default).",
            required = false
        )
        String priority,

        // ====== LIST MODE PARAMETERS ======
        @McpParam(
            description = """
                [LIST MODE] Filter todos by their current status.

                Status values:
                - pending: Not started, awaiting action
                - in_progress: Currently being worked on
                - completed: Finished tasks
                - null/empty: Show all todos regardless of status
                """,
            examples = {"pending", "in_progress", "completed", ""},
            constraints = "Optional for 'list' mode. Must be valid status or empty. Default shows all.",
            hints = "Use this to focus on specific workflow stages. Empty value shows everything.",
            required = false
        )
        String status,

        @McpParam(
            description = """
                [LIST MODE] Sort order for displaying todos.

                Sort options:
                - status: Group by status (pending -> in_progress -> completed)
                - date: Chronological by creation date (newest first)
                - priority: By priority (critical -> high -> medium -> low)
                - alpha: Alphabetical by task description
                """,
            examples = {"status", "date", "priority", "alpha"},
            constraints = "Optional for 'list' mode. Must be one of: status, date, priority, alpha. Default is 'status'.",
            hints = "Choose sort order based on what you need to see. 'status' is most common.",
            required = false
        )
        String sort,

        // ====== UPDATE MODE PARAMETERS ======
        @McpParam(
            description = """
                [UPDATE MODE] The unique identifier of the todo to modify.

                Where to find the ID:
                - From the 'list' mode output (shown in [ID] format)
                - From previous add operation response
                - IDs are alphanumeric strings like 'abc12345'
                """,
            examples = {"abc12345", "def67890", "xyz99999"},
            constraints = "Required for 'update' mode. Must be a valid existing todo ID.",
            hints = "Use 'list' mode first to find the ID if you don't know it.",
            required = false
        )
        String id,

        @McpParam(
            description = """
                [UPDATE MODE] The new status to assign to the todo.

                Status transitions:
                - pending: Task not started
                - in_progress: Currently working on this task
                - completed: Task is finished

                Common workflow: pending -> in_progress -> completed
                """,
            examples = {"completed", "in_progress", "pending"},
            constraints = "Optional for 'update' mode. Must be one of: pending, in_progress, completed.",
            hints = "Use 'in_progress' when starting work, 'completed' when done. Can move back to 'pending' if needed.",
            required = false
        )
        String newStatus,

        @McpParam(
            description = """
                [UPDATE MODE] New task description to replace the existing one.

                When to use:
                - Fix typos in original task
                - Add more clarity or detail
                - Adjust scope of work
                - Clarify acceptance criteria
                """,
            examples = {
                "Implement OAuth2 authentication (updated scope)",
                "Write comprehensive unit tests for PaymentService (edge cases included)",
                "Deploy to production with database migration v2.3"
            },
            constraints = "Optional for 'update' mode. Only use when changing task text. Cannot be empty.",
            hints = "Only provide this parameter when you want to change the task description. Leave empty to only change status.",
            required = false
        )
        String newTask,

        // ====== DELETE MODE PARAMETERS ======
        @McpParam(
            description = """
                [DELETE MODE] The unique identifier of the todo to permanently remove.

                Warning: This action cannot be undone!

                Where to find the ID:
                - From 'list' mode output (shown in [ID] format)
                - From previous responses
                - IDs are alphanumeric like 'abc12345'
                """,
            examples = {"abc12345", "def67890", "xyz99999"},
            constraints = "Required for 'delete' mode. Must be a valid existing todo ID.",
            hints = "Double-check the ID before deleting. Consider using 'update' to mark as completed instead if unsure.",
            required = false
        )
        String deleteId,

        // ====== CLEARCOMPLETED MODE PARAMETERS ======
        // No additional parameters needed for clearCompleted mode

        @McpParam(
            description = """
                [CLEARCOMPLETED MODE] Optional confirmation flag for safety.

                This mode deletes ALL completed todos at once.
                Set to 'true' to confirm the bulk delete operation.
                """,
            examples = {"true", "false"},
            constraints = "Optional for 'clearCompleted' mode. Boolean value. Default is true.",
            hints = "This is a safety parameter. The operation will proceed with default true, but explicit confirmation is recommended for large lists.",
            required = false
        )
        String confirm
    ) {
        // Route to appropriate mode handler
        return switch (mode.toLowerCase()) {
            case "add" -> handleAdd(task, priority);
            case "list" -> handleList(status, sort);
            case "update" -> handleUpdate(id, newStatus, newTask);
            case "delete" -> handleDelete(deleteId);
            case "clearcompleted" -> handleClearCompleted(confirm);
            default -> throw new TodoException(
                "Invalid mode: '%s'. Valid modes are: add, list, update, delete, clearCompleted".formatted(mode)
            );
        };
    }

    /**
     * Handle the 'add' mode - create a new todo.
     */
    private String handleAdd(String task, String priority) {
        // Validate required parameters
        if (task == null || task.isBlank()) {
            throw new TodoException("Parameter 'task' is required for 'add' mode. Provide a task description.");
        }

        // Parse priority
        TodoPriority prio = TodoPriority.fromString(priority);

        // Create the todo
        String id = store.add(task, prio);
        String response = String.format("Added todo (ID: %s): %s [Priority: %s]",
            id, task, prio.getDisplayName());

        return response;
    }

    /**
     * Handle the 'list' mode - display todos.
     */
    private String handleList(String status, String sort) {
        TodoStatus filter = parseStatus(status);

        // Get todos
        List<TodoItem> todos = store.list(filter);

        // Apply sorting if specified
        if (sort != null && !sort.isBlank()) {
            todos = sortTodos(todos, sort);
        }

        if (todos.isEmpty()) {
            return String.format("No %stodos found.", filter != null ? filter.toString().toLowerCase() + " " : "");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(formatHeader(filter, sort));

        for (TodoItem todo : todos) {
            sb.append(formatTodo(todo));
        }

        sb.append(formatSummary());

        return sb.toString();
    }

    /**
     * Handle the 'update' mode - modify an existing todo.
     */
    private String handleUpdate(String id, String newStatus, String newTask) {
        // At least one of newStatus or newTask must be provided
        if (id == null || id.isBlank()) {
            throw new TodoException("Parameter 'id' is required for 'update' mode. Provide the todo ID to update.");
        }

        if (newStatus == null || newStatus.isBlank()) {
            if (newTask == null || newTask.isBlank()) {
                throw new TodoException("At least one of 'newStatus' or 'newTask' is required for 'update' mode.");
            }
            // Only updating task text
            store.updateTask(id, newTask);
            return String.format("Updated todo (ID: %s): %s", id, newTask);
        }

        // Parse and validate status
        TodoStatus status = parseStatus(newStatus);
        if (status == null) {
            throw new TodoException("Invalid status: '%s'. Use: pending, in_progress, completed".formatted(newStatus));
        }

        // Check if todo exists
        TodoItem existing = store.get(id);
        if (existing == null) {
            throw new TodoException("Todo not found with ID: %s. Use 'list' mode to see valid IDs.".formatted(id));
        }

        // Update status
        store.updateStatus(id, status);

        // Build response
        String response = String.format("Updated todo %s: %s -> %s",
            id,
            existing.status().toString().toLowerCase(),
            status.toString().toLowerCase()
        );

        // Add task update if provided
        if (newTask != null && !newTask.isBlank()) {
            store.updateTask(id, newTask);
            response += String.format("\nTask description updated to: %s", newTask);
        }

        return response;
    }

    /**
     * Handle the 'delete' mode - remove a todo.
     */
    private String handleDelete(String deleteId) {
        // Validate required parameters
        if (deleteId == null || deleteId.isBlank()) {
            throw new TodoException("Parameter 'deleteId' is required for 'delete' mode. Provide the todo ID to delete.");
        }

        // Check if todo exists
        TodoItem existing = store.get(deleteId);
        if (existing == null) {
            throw new TodoException("Todo not found with ID: %s. Use 'list' mode to see valid IDs.".formatted(deleteId));
        }

        // Delete the todo
        store.delete(deleteId);
        return String.format("Deleted todo (ID: %s): %s", deleteId, existing.task());
    }

    /**
     * Handle the 'clearCompleted' mode - bulk delete completed todos.
     */
    private String handleClearCompleted(String confirm) {
        int completedCount = store.count(TodoStatus.COMPLETED);

        if (completedCount == 0) {
            return "No completed todos to clear.";
        }

        // Clear completed todos
        store.clearCompleted();
        return String.format("Cleared %d completed todo(s).", completedCount);
    }

    /**
     * Sort todos based on the specified sort order.
     */
    private List<TodoItem> sortTodos(List<TodoItem> todos, String sort) {
        // Create a mutable copy
        List<TodoItem> sorted = new ArrayList<>(todos);

        return switch (sort.toLowerCase()) {
            case "status" -> sorted.stream()
                .sorted(Comparator.comparing(TodoItem::status))
                .toList();

            case "date" -> sorted.stream()
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt())) // Newest first
                .toList();

            case "priority" -> sorted.stream()
                .sorted(Comparator.comparing((TodoItem t) -> t.priority().getValue()).reversed())
                .toList();

            case "alpha" -> sorted.stream()
                .sorted(Comparator.comparing(TodoItem::task, String.CASE_INSENSITIVE_ORDER))
                .toList();

            default -> sorted;
        };
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

    private String formatHeader(TodoStatus filter, String sort) {
        int total = store.count(null);
        int pending = store.count(TodoStatus.PENDING);
        int inProgress = store.count(TodoStatus.IN_PROGRESS);
        int completed = store.count(TodoStatus.COMPLETED);

        StringBuilder sb = new StringBuilder();
        sb.append("Todo List\n");
        sb.append("=".repeat(40)).append("\n");

        if (filter != null) {
            sb.append("Filtered by: ").append(filter.toString().toLowerCase()).append("\n");
        }

        if (sort != null && !sort.isBlank()) {
            sb.append("Sorted by: ").append(sort).append("\n");
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

        String prioIcon = switch (todo.priority()) {
            case LOW -> "🟢";
            case MEDIUM -> "🟡";
            case HIGH -> "🟠";
            case CRITICAL -> "🔴";
        };

        return String.format("%s %s [%s] %s\n",
            prioIcon,
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
