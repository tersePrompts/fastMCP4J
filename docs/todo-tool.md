# Todo Tool Feature

FastMCP4J provides a Todo tool that allows AI agents to track and manage tasks with status tracking (pending, in_progress, completed).

## Features

### Capabilities
- **Add todos** - Create new task items with descriptions
- **List todos** - View all tasks or filter by status
- **Update status** - Change task status (pending → in_progress → completed)
- **Delete todos** - Remove individual tasks
- **Clear completed** - Remove all completed tasks at once

### Task Status

| Status | Icon | Description |
|---------|--------|-------------|
| PENDING | ⏳ | Task not started yet |
| IN_PROGRESS | 🔄 | Task currently being worked on |
| COMPLETED | ✅ | Task finished |

## Usage

### Enable Todo Tool

Add the `@McpTodo` annotation to your server class:

```java
import com.ultrathink.fastmcp.annotations.McpServer;
import com.ultrathink.fastmcp.annotations.McpTodo;
import com.ultrathink.fastmcp.core.FastMCP;

@McpServer(name = "My Server")
@McpTodo
public class MyServer {
    
    // Your custom business tools
    
    public static void main(String[] args) {
        FastMCP.server(MyServer.class)
            .stdio()
            .run();
    }
}
```

### Todo Tool Commands

#### Add Todo

```java
// The AI agent calls:
add("Implement user authentication")

// Response:
"Added todo (ID: abc123): Implement user authentication"
```

#### List Todos

```java
// List all todos
list(null)
// Response:
📋 Todo List
========================================
Total: 3 | Pending: 2 | In Progress: 1 | Completed: 0
========================================

⏳ [abc123] Implement user authentication
🔄 [def456] Write unit tests
⏳ [ghi789] Deploy to production

----------------------------------------
Next: Start a pending task or update status
Summary: 2 pending, 1 in progress, 0 completed

// List only completed todos
list("completed")
// Response:
📋 Todo List
========================================
Filtered by: completed
Total: 1 | Pending: 0 | In Progress: 0 | Completed: 1
========================================

✅ [jkl012] Setup project structure
```

#### Update Status

```java
// Mark task as in progress
update("abc123", "in_progress")
// Response:
"Updated todo abc123: pending -> in_progress"

// Mark task as completed
update("abc123", "completed")
// Response:
"Updated todo abc123: in_progress -> completed"
```

#### Delete Todo

```java
delete("abc123")
// Response:
"Deleted todo (ID: abc123): Implement user authentication"
```

#### Clear Completed

```java
clearCompleted()
// Response:
"Cleared 3 completed todo(s)."
```

## Custom Store

By default, todos are stored in-memory using thread-safe collections (`ConcurrentHashMap` and `CopyOnWriteArrayList`). For custom persistence:

```java
import com.ultrathink.fastmcp.todo.TodoStore;

public class DatabaseTodoStore implements TodoStore {
    // Implement all interface methods
    // Store in database, file system, etc.
}

// Use custom store
FastMCP.server(MyServer.class)
    .todoStore(new DatabaseTodoStore())
    .stdio()
    .run();
```

## Implementation

### Files

- `todo/TodoStore.java` - High-level interface for todo storage
- `todo/TodoItem.java` - Todo item record with metadata
- `todo/TodoStatus.java` - Todo status enum (PENDING, IN_PROGRESS, COMPLETED)
- `todo/InMemoryTodoStore.java` - Thread-safe in-memory implementation
- `todo/TodoException.java` - Exception for todo-related errors
- `todo/TodoTool.java` - Tool using `@McpTool` annotations
- `annotations/McpTodo.java` - Annotation to enable todo tool

### Thread Safety

The default `InMemoryTodoStore` uses:
- `ConcurrentHashMap<String, TodoItem>` - O(1) lookups by ID
- `CopyOnWriteArrayList<TodoItem>` - Thread-safe iteration over all todos

Both collections are safe for concurrent reads and writes from multiple threads.

## Example Server

See `src/test/java/com/ultrathink/fastmcp/example/TodoExampleServer.java` for a complete example.

## Best Practices

Based on research into AI agent task management patterns [^1][^2]:

1. **Use for tracking**, not planning** - Todo tool tracks task state; use Planner tool for decomposition
2. **Clear completed regularly** - Keeps todo list focused and relevant
3. **Descriptive tasks** - Use clear, actionable task descriptions
4. **Status progression** - Follow PENDING → IN_PROGRESS → COMPLETED workflow

[^1]: https://github.com/eyaltoledano/claude-task-master (Claude Task Master patterns)
[^2]: https://towardsdatascience.com/how-agents-plan-tasks-with-to-do-lists (Agent task planning)
