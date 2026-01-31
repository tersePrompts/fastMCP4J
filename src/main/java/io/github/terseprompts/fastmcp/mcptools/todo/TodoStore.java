package io.github.terseprompts.fastmcp.mcptools.todo;

import java.util.List;

/**
 * High-level interface for todo storage.
 * <p>
 * Implementations can store todos in memory, database, files, etc.
 * The default {@link InMemoryTodoStore} uses thread-safe in-memory storage.
 */
public interface TodoStore {

    /**
     * Add a new todo item.
     *
     * @param task the task description
     * @return the todo ID
     */
    String add(String task);

    /**
     * Add a new todo item with priority.
     *
     * @param task the task description
     * @param priority the priority level
     * @return the todo ID
     */
    String add(String task, TodoPriority priority);

    /**
     * List all todo items, optionally filtered by status.
     *
     * @param status optional status filter (null for all)
     * @return list of todos
     */
    List<TodoItem> list(TodoStatus status);

    /**
     * Update a todo item's status.
     *
     * @param id the todo ID
     * @param status the new status
     */
    void updateStatus(String id, TodoStatus status);

    /**
     * Update a todo item's task description.
     *
     * @param id the todo ID
     * @param task the new task description
     */
    void updateTask(String id, String task);

    /**
     * Delete a todo item.
     *
     * @param id the todo ID
     */
    void delete(String id);

    /**
     * Clear all completed todos.
     */
    void clearCompleted();

    /**
     * Get a todo item by ID.
     *
     * @param id the todo ID
     * @return the todo item, or null if not found
     */
    TodoItem get(String id);

    /**
     * Get count of todos by status.
     *
     * @param status the status
     * @return count
     */
    int count(TodoStatus status);
}
