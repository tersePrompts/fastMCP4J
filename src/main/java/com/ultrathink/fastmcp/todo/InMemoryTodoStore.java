package com.ultrathink.fastmcp.todo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory todo store using concurrent collections.
 * <p>
 * Uses ConcurrentHashMap for O(1) lookups by ID and CopyOnWriteArrayList
 * for thread-safe iteration over all todos.
 */
public class InMemoryTodoStore implements TodoStore {

    private final Map<String, TodoItem> todosById;
    private final List<TodoItem> todos;

    public InMemoryTodoStore() {
        this.todosById = new ConcurrentHashMap<>();
        this.todos = new CopyOnWriteArrayList<>();
    }

    @Override
    public String add(String task) {
        return add(task, TodoPriority.MEDIUM); // Default priority
    }

    @Override
    public String add(String task, TodoPriority priority) {
        String id = generateId();
        Instant now = Instant.now();
        TodoItem todo = new TodoItem(id, task, TodoStatus.PENDING, now, now, priority);

        todosById.put(id, todo);
        todos.add(todo);

        return id;
    }

    @Override
    public List<TodoItem> list(TodoStatus status) {
        if (status == null) {
            return new ArrayList<>(todos);
        }
        return todos.stream()
            .filter(todo -> todo.status() == status)
            .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(String id, TodoStatus status) {
        TodoItem existing = todosById.get(id);
        if (existing == null) {
            throw new TodoException("Todo not found: " + id);
        }

        TodoItem updated = new TodoItem(
            existing.id(),
            existing.task(),
            status,
            existing.createdAt(),
            Instant.now(),
            existing.priority()  // Preserve priority
        );

        todosById.put(id, updated);

        // Update in list (remove old, add new to maintain order)
        todos.removeIf(todo -> todo.id().equals(id));
        todos.add(updated);
    }

    @Override
    public void updateTask(String id, String task) {
        TodoItem existing = todosById.get(id);
        if (existing == null) {
            throw new TodoException("Todo not found: " + id);
        }

        if (task == null || task.isBlank()) {
            throw new IllegalArgumentException("Task cannot be null or blank");
        }

        TodoItem updated = new TodoItem(
            existing.id(),
            task,
            existing.status(),
            existing.createdAt(),
            Instant.now(),
            existing.priority()  // Preserve priority
        );

        todosById.put(id, updated);

        // Update in list (remove old, add new to maintain order)
        todos.removeIf(todo -> todo.id().equals(id));
        todos.add(updated);
    }

    @Override
    public void delete(String id) {
        TodoItem removed = todosById.remove(id);
        if (removed == null) {
            throw new TodoException("Todo not found: " + id);
        }
        todos.removeIf(todo -> todo.id().equals(id));
    }

    @Override
    public void clearCompleted() {
        List<String> completedIds = todos.stream()
            .filter(todo -> todo.status() == TodoStatus.COMPLETED)
            .map(TodoItem::id)
            .collect(Collectors.toList());

        completedIds.forEach(id -> {
            todosById.remove(id);
            todos.removeIf(todo -> todo.id().equals(id));
        });
    }

    @Override
    public TodoItem get(String id) {
        return todosById.get(id);
    }

    @Override
    public int count(TodoStatus status) {
        if (status == null) {
            return todos.size();
        }
        return (int) todos.stream()
            .filter(todo -> todo.status() == status)
            .count();
    }

    private String generateId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
