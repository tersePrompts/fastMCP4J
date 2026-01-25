# Planner Tool Feature

FastMCP4J provides a Planner tool that enables AI agents to decompose complex tasks into hierarchical, executable plans with dependencies and execution tracking.

## Features

### Capabilities
- **Create plans** - Define execution plans with root-level tasks
- **Task decomposition** - Break down tasks into subtasks (hierarchical structure)
- **Dependencies** - Define task dependencies for ordered execution
- **Execution types** - Sequential (one at a time) or parallel execution
- **Status tracking** - Track task progress (PENDING, IN_PROGRESS, COMPLETED, FAILED, BLOCKED)
- **Next task** - Get the next pending task that can execute (dependencies satisfied)

### Task Status

| Status | Icon | Description |
|---------|--------|-------------|
| PENDING | ⏳ | Task waiting to start |
| IN_PROGRESS | 🔄 | Task currently executing |
| COMPLETED | ✅ | Task finished successfully |
| FAILED | ❌ | Task execution failed |
| BLOCKED | 🚫 | Task blocked by incomplete dependencies or issues |

### Execution Types

| Type | Description |
|-------|-------------|
| SEQUENTIAL | Execute tasks one at a time |
| PARALLEL | Can execute alongside other parallel tasks |

## Usage

### Enable Planner Tool

Add `@McpPlanner` annotation to your server class:

```java
import com.ultrathink.fastmcp.annotations.McpServer;
import com.ultrathink.fastmcp.annotations.McpPlanner;
import com.ultrathink.fastmcp.core.FastMCP;

@McpServer(name = "My Server")
@McpPlanner
public class MyServer {
    
    // Your custom business tools
    
    public static void main(String[] args) {
        FastMCP.server(MyServer.class)
            .stdio()
            .run();
    }
}
```

### Planner Tool Commands

#### Create Plan

```java
// Create a plan with initial tasks
createPlan(
    "Build REST API",
    "Implement user management endpoints with CRUD operations",
    List.of()
)

// Response:
"Created plan 'Build REST API' (ID: abc123) with 0 root task(s)"
```

#### Add Tasks to Plan

```java
// Add a root task
addTask("abc123", "Setup project structure", "Create directories and Maven modules", "sequential", List.of())

// Response:
"Added task 'Setup project structure' (ID: def456) to plan abc123"

// Add a task with dependencies
addTask("abc123", "Implement endpoints", "Create user CRUD API", "sequential", List.of("def456"))

// Response:
"Added task 'Implement endpoints' (ID: ghi789) to plan abc123"
```

#### Add Subtasks

```java
// Break down a task into smaller steps
addSubtask("abc123", "def456", "Create Maven pom.xml", "Configure dependencies and build")

// Response:
"Added subtask 'Create Maven pom.xml' (ID: jkl012) to task def456"
```

#### Get Plan Details

```java
// View full plan with task hierarchy
getPlan("abc123")

// Response:
📋 Plan: Build REST API
============================================================
ID: abc123
Status: pending
Description: Implement user management endpoints with CRUD operations
Progress: 0/4 tasks complete
============================================================

⏳ Setup project structure [SEQUENTIAL]
  ⏳ Create Maven pom.xml
  ⏳ Create directory structure
⏳ Implement endpoints [SEQUENTIAL] (deps: def456)
  ⏳ Create User entity
  ⏳ Implement UserController
⏳ Write tests (deps: ghi789)
  ⏳ Unit tests
  ⏳ Integration tests
```

#### List Plans

```java
// List all plans
listPlans()

// Response:
📋 Plans
==================================================
⏳ [abc123] Build REST API
   Implement user management endpoints with CRUD operations
   Status: pending | Tasks: 0/4 complete
✅ [def456] Migrate Database
   Migrate from PostgreSQL to MongoDB
   Status: completed | Tasks: 3/3 complete
```

#### Update Task Status

```java
// Mark task as in progress
updateTask("abc123", "def456", "in_progress")
// Response:
"Updated task def456 status to in_progress"

// Mark task as completed
updateTask("abc123", "def456", "completed")
// Response:
"Updated task def456 status to completed"
```

#### Get Next Task

```java
// Get the next task that can execute (dependencies satisfied)
getNextTask("abc123")
// Response:
"🎯 Next task: Setup project structure
Create directories and Maven modules
ID: def456"

// When all tasks complete
getNextTask("abc123")
// Response:
"✅ All 4 tasks in plan 'Build REST API' are complete!"
```

#### Delete Plan

```java
// Delete a plan and all its tasks
deletePlan("abc123")
// Response:
"Deleted plan 'Build REST API' (ID: abc123) with 4 task(s)"
```

## Planning Patterns

### Task Decomposition

Break complex tasks into smaller, manageable subtasks:

```java
// High-level task
addTask("plan123", "Build Web App", "Create full-stack application", "sequential", List.of())

// Decompose into subtasks
addSubtask("plan123", "root1", "Frontend", "React UI with routing")
addSubtask("plan123", "root1", "Backend", "REST API with auth")
addSubtask("plan123", "root1", "Database", "PostgreSQL schema design")
```

### Dependencies

Define tasks that must complete before others:

```java
// Initial task (no dependencies)
addTask("plan123", "Setup database", "Create schema and seed data", "sequential", List.of())

// Task that depends on database
addTask("plan123", "Implement API", "CRUD endpoints", "sequential", List.of("task1"))

// Task that depends on both
addTask("plan123", "Write tests", "Test coverage", "sequential", List.of("task2"))
```

### Parallel Execution

Tasks that can run concurrently:

```java
// These can execute in parallel
addTask("plan123", "Write unit tests", "Test individual components", "parallel", List.of())
addTask("plan123", "Generate docs", "Create API documentation", "parallel", List.of())
```

## Custom Store

By default, plans are stored in-memory using thread-safe collections. For custom persistence:

```java
import com.ultrathink.fastmcp.planner.PlanStore;

public class DatabasePlanStore implements PlanStore {
    // Implement all interface methods
    // Store in database, file system, etc.
}

// Use custom store
FastMCP.server(MyServer.class)
    .planStore(new DatabasePlanStore())
    .stdio()
    .run();
```

## Implementation

### Files

- `planner/PlanStore.java` - High-level interface for plan storage
- `planner/InMemoryPlanStore.java` - Thread-safe in-memory implementation
- `planner/PlannerException.java` - Exception for planner-related errors
- `planner/PlannerTool.java` - Tool using `@McpTool` annotations
- `annotations/McpPlanner.java` - Annotation to enable planner tool

### Thread Safety

The default `InMemoryPlanStore` uses:
- `ConcurrentHashMap<String, Plan>` - Plan storage by ID
- `ConcurrentHashMap<String, Task>` - Task storage by ID
- `CopyOnWriteArrayList<Plan>` - Thread-safe iteration over plans

## Design Principles

Based on research into AI agent planning patterns [^1][^2][^3][^4]:

### Task Decomposition
Complex problems are broken down into smaller, manageable subtasks. This:
- Reduces cognitive load on the AI
- Enables parallel execution where possible
- Makes progress tracking easier
- Allows for iterative refinement

### Hierarchical Planning
Tasks can have subtasks, creating a tree structure:
- `Plan` → `Task` → `Subtask` → `Sub-subtask`
- Each level can have its own dependencies
- Supports recursive decomposition

### Dependencies
Tasks define dependencies on other tasks:
- Must complete before dependent task can start
- Enables ordered execution
- Prevents incomplete state

### Memory-Aided Planning
Plans persist across sessions:
- Context for long-running workflows
- Resume capability after interruptions
- Shared state across tools

## Example Server

See `src/test/java/com/ultrathink/fastmcp/example/PlannerExampleServer.java` for a complete example.

## Comparison: Planner vs Todo

| Feature | Planner Tool | Todo Tool |
|----------|---------------|-------------|
| **Purpose** | Task decomposition and execution planning | Simple task tracking |
| **Structure** | Hierarchical (plans → tasks → subtasks) | Flat list |
| **Dependencies** | Yes, tasks can depend on others | No |
| **Execution** | Sequential/parallel with ordering | Linear progression |
| **Use case** | Complex multi-step workflows | Simple task lists |
| **Best for** | Software projects, workflows | Daily tasks, quick lists |

## Best Practices

1. **Start high-level** - Create the plan first, then add tasks
2. **Decompose appropriately** - Break down until tasks are actionable
3. **Use dependencies** - Define real dependencies, not just order
4. **Mark in-progress** - Set status to IN_PROGRESS when starting
5. **Update status promptly** - Mark COMPLETED or FAILED after execution

## Research References

[^1]: https://arxiv.org/pdf/2402.02716 (Task decomposition frameworks: Divide and Conquer, CoT, ReAct)
[^2]: https://www.linkedin.com/pulse/task-decomposition-autonomous-ai-agents-principles-andre-9nmee (Multi-agent coordination)
[^3]: https://www.analyticsvidhya.com/blog/2024/11/agentic-ai-planning-pattern (AI agent planning patterns)
[^4]: https://aclanthology.org/2024.findings-naacl.264 (As-Needed Decomposition and Planning - ADaPT)
