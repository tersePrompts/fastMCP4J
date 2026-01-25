package com.ultrathink.fastmcp.planner;

import com.ultrathink.fastmcp.annotations.McpParam;
import com.ultrathink.fastmcp.annotations.McpTool;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Planner tool using FastMCP4J annotations.
 * <p>
 * Provides task decomposition and planning capabilities:
 * - Create hierarchical plans
 * - Add tasks and subtasks
 * - Update task status
 * - Track plan execution progress
 * - Get next pending task
 * <p>
 * Usage: Add {@code @McpPlanner} to server class to enable.
 * <p>
 * Based on research into Claude planner and MCP task management patterns:
 * - Task decomposition for complex problems [^1]
 * - Hierarchical planning with dependencies [^2]
 * - Memory-aided planning for context [^3]
 * - As-needed decomposition (ADaPT) [^4]
 *
 * @see com.ultrathink.fastmcp.annotations.McpPlanner
 *
 * [^1]: https://arxiv.org/pdf/2402.02716 (Task decomposition frameworks)
 * [^2]: https://www.linkedin.com/pulse/task-decomposition-autonomous-ai-agents-principles-andre-9nmee
 * [^3]: https://www.analyticsvidhya.com/blog/2024/11/agentic-ai-planning-pattern
 * [^4]: https://aclanthology.org/2024.findings-naacl.264 (As-Needed Decomposition and Planning)
 */
public class PlannerTool {

    private final PlanStore store;

    public PlannerTool(PlanStore store) {
        this.store = store;
    }

    /**
     * Create a new execution plan.
     */
    @McpTool(description = "Create a new plan for decomposing a complex task into smaller steps")
    public String createPlan(
        @McpParam(
            description = "The plan name/title",
            examples = {"Build REST API", "Migrate database", "Implement authentication"},
            constraints = "Cannot be empty"
        )
        String name,

        @McpParam(
            description = "Detailed description of what the plan aims to accomplish",
            examples = {"Implement user management with CRUD operations", "Migrate from PostgreSQL to MongoDB"},
            constraints = "Should explain the goal and context"
        )
        String description,

        @McpParam(
            description = "Initial root-level tasks as a list. Each task is a JSON object with 'title', 'description', and optionally 'execution_type' and 'dependencies'",
            examples = {"[{\"title\": \"Setup project structure\", \"description\": \"Create directories and files\"}, {\"title\": \"Implement API endpoints\", \"description\": \"CRUD operations\", \"execution_type\": \"sequential\"}]"},
            required = false
        )
        List<String> initialTasks
    ) {
        List<PlanStore.Task> tasks = new ArrayList<>();
        if (initialTasks != null) {
            for (String taskJson : initialTasks) {
                PlanStore.Task task = parseTaskFromJson(taskJson);
                tasks.add(task);
            }
        }

        String planId = store.createPlan(name, description, tasks);
        return String.format("Created plan '%s' (ID: %s) with %d root task(s)%n%s",
            name, planId, tasks.size(),
            tasks.isEmpty() ? "Use addTask() to add tasks." : "");
    }

    /**
     * List all plans.
     */
    @McpTool(description = "List all available plans with their status and summary")
    public String listPlans() {
        List<PlanStore.Plan> plans = store.listPlans();

        if (plans.isEmpty()) {
            return "No plans found. Use createPlan() to create a new plan.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 Plans\n");
        sb.append("=".repeat(50)).append("\n");

        for (PlanStore.Plan plan : plans) {
            sb.append(formatPlanSummary(plan));
        }

        return sb.toString();
    }

    /**
     * Get details of a specific plan.
     */
    @McpTool(description = "Get detailed view of a plan including all tasks in hierarchy")
    public String getPlan(
        @McpParam(
            description = "The plan ID to retrieve",
            examples = {"abc12345", "def67890"}
        )
        String planId
    ) {
        PlanStore.Plan plan = store.getPlan(planId);
        if (plan == null) {
            throw new PlannerException("Plan not found: " + planId);
        }

        return formatPlanDetails(plan);
    }

    /**
     * Add a new task to a plan.
     */
    @McpTool(description = "Add a new task to an existing plan")
    public String addTask(
        @McpParam(
            description = "The plan ID to add the task to",
            examples = {"abc12345", "def67890"}
        )
        String planId,

        @McpParam(
            description = "Task title (short description)",
            examples = {"Setup database", "Write unit tests", "Deploy to staging"}
        )
        String title,

        @McpParam(
            description = "Detailed task description",
            examples = {"Configure PostgreSQL connection", "Test all API endpoints"},
            required = false
        )
        String description,

        @McpParam(
            description = "Execution type: 'sequential' or 'parallel'",
            examples = {"sequential", "parallel"},
            defaultValue = "sequential",
            required = false
        )
        String executionType,

        @McpParam(
            description = "Task dependencies (list of task IDs that must complete first)",
            examples = {"[\"task1\", \"task2\"]"},
            required = false
        )
        List<String> dependencies
    ) {
        PlanStore.TaskExecutionType execType = parseExecutionType(executionType);

        PlanStore.Task task = new PlanStore.Task(
            null, // ID will be assigned
            title,
            description != null ? description : "",
            PlanStore.TaskStatus.PENDING,
            execType,
            dependencies != null ? dependencies : new ArrayList<>(),
            new ArrayList<>(),
            java.time.Instant.now(),
            java.time.Instant.now()
        );

        String taskId = store.addTask(planId, task);
        return String.format("Added task '%s' (ID: %s) to plan %s", title, taskId, planId);
    }

    /**
     * Add a subtask to an existing task.
     */
    @McpTool(description = "Add a subtask to break down a task into smaller steps")
    public String addSubtask(
        @McpParam(
            description = "The plan ID",
            examples = {"abc12345"}
        )
        String planId,

        @McpParam(
            description = "The parent task ID to add subtask to",
            examples = {"task1", "task2"}
        )
        String parentTaskId,

        @McpParam(
            description = "Subtask title",
            examples = {"Configure connection", "Write tests", "Verify output"}
        )
        String title,

        @McpParam(
            description = "Detailed subtask description",
            examples = {"Setup database schema", "Test edge cases"},
            required = false
        )
        String description
    ) {
        PlanStore.Task subtask = new PlanStore.Task(
            null,
            title,
            description != null ? description : "",
            PlanStore.TaskStatus.PENDING,
            PlanStore.TaskExecutionType.SEQUENTIAL,
            new ArrayList<>(),
            new ArrayList<>(),
            java.time.Instant.now(),
            java.time.Instant.now()
        );

        String subtaskId = store.addSubtask(planId, parentTaskId, subtask);
        return String.format("Added subtask '%s' (ID: %s) to task %s", title, subtaskId, parentTaskId);
    }

    /**
     * Update task status.
     */
    @McpTool(description = "Update the status of a task in a plan")
    public String updateTask(
        @McpParam(
            description = "The plan ID",
            examples = {"abc12345"}
        )
        String planId,

        @McpParam(
            description = "The task ID to update",
            examples = {"task1", "task2"}
        )
        String taskId,

        @McpParam(
            description = "New status: 'pending', 'in_progress', 'completed', 'failed', or 'blocked'",
            examples = {"completed", "in_progress", "failed"},
            constraints = "Must be a valid status value"
        )
        String status
    ) {
        PlanStore.TaskStatus newStatus = parseStatus(status);
        if (newStatus == null) {
            throw new PlannerException("Invalid status: " + status + ". Use: pending, in_progress, completed, failed, blocked");
        }

        store.updateTaskStatus(planId, taskId, newStatus);
        return String.format("Updated task %s status to %s", taskId, newStatus.toString().toLowerCase());
    }

    /**
     * Get the next task to work on.
     */
    @McpTool(description = "Get the next pending task that can be executed (all dependencies complete)")
    public String getNextTask(
        @McpParam(
            description = "The plan ID",
            examples = {"abc12345"}
        )
        String planId
    ) {
        PlanStore.Task nextTask = store.getNextTask(planId);

        if (nextTask == null) {
            PlanStore.Plan plan = store.getPlan(planId);
            if (plan == null) {
                throw new PlannerException("Plan not found: " + planId);
            }

            long completedTasks = store.getAllTasks(planId).stream()
                .filter(t -> t.status() == PlanStore.TaskStatus.COMPLETED)
                .count();
            long totalTasks = store.getAllTasks(planId).size();

            if (completedTasks == totalTasks && totalTasks > 0) {
                return String.format("✅ All %d tasks in plan '%s' are complete!", totalTasks, plan.name());
            }
            return "No pending tasks available. Check if there are blocked tasks or dependencies.";
        }

        return String.format("🎯 Next task: %s%n%s%nID: %s",
            nextTask.title(),
            nextTask.description() != null && !nextTask.description().isBlank() ? nextTask.description() + "\n" : "",
            nextTask.id());
    }

    /**
     * Delete a plan.
     */
    @McpTool(description = "Delete a plan and all its tasks")
    public String deletePlan(
        @McpParam(
            description = "The plan ID to delete",
            examples = {"abc12345"}
        )
        String planId
    ) {
        PlanStore.Plan plan = store.getPlan(planId);
        if (plan == null) {
            throw new PlannerException("Plan not found: " + planId);
        }

        int taskCount = store.getAllTasks(planId).size();
        store.deletePlan(planId);
        return String.format("Deleted plan '%s' (ID: %s) with %d task(s)", plan.name(), planId, taskCount);
    }

    private PlanStore.TaskExecutionType parseExecutionType(String type) {
        if (type == null || type.isBlank()) {
            return PlanStore.TaskExecutionType.SEQUENTIAL;
        }
        return switch (type.toLowerCase()) {
            case "parallel" -> PlanStore.TaskExecutionType.PARALLEL;
            default -> PlanStore.TaskExecutionType.SEQUENTIAL;
        };
    }

    private PlanStore.TaskStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.toLowerCase()) {
            case "pending" -> PlanStore.TaskStatus.PENDING;
            case "in_progress" -> PlanStore.TaskStatus.IN_PROGRESS;
            case "completed", "complete", "done" -> PlanStore.TaskStatus.COMPLETED;
            case "failed" -> PlanStore.TaskStatus.FAILED;
            case "blocked" -> PlanStore.TaskStatus.BLOCKED;
            default -> throw new PlannerException("Invalid status: " + status);
        };
    }

    private String formatPlanSummary(PlanStore.Plan plan) {
        long totalTasks = store.getAllTasks(plan.id()).size();
        long completedTasks = store.getAllTasks(plan.id()).stream()
            .filter(t -> t.status() == PlanStore.TaskStatus.COMPLETED)
            .count();

        String statusIcon = switch (plan.status()) {
            case PENDING -> "⏳";
            case IN_PROGRESS -> "🔄";
            case COMPLETED -> "✅";
            case FAILED -> "❌";
            case BLOCKED -> "🚫";
        };

        return String.format("%s [%s] %s%n   %s%n   Status: %s | Tasks: %d/%d complete%n",
            statusIcon,
            plan.id(),
            plan.name(),
            plan.description(),
            plan.status().toString().toLowerCase(),
            completedTasks,
            totalTasks);
    }

    private String formatPlanDetails(PlanStore.Plan plan) {
        List<PlanStore.Task> allTasks = store.getAllTasks(plan.id());
        long completedTasks = allTasks.stream()
            .filter(t -> t.status() == PlanStore.TaskStatus.COMPLETED)
            .count();

        StringBuilder sb = new StringBuilder();
        sb.append("📋 Plan: ").append(plan.name()).append("\n");
        sb.append("=".repeat(60)).append("\n");
        sb.append("ID: ").append(plan.id()).append("\n");
        sb.append("Status: ").append(plan.status().toString().toLowerCase()).append("\n");
        sb.append("Description: ").append(plan.description()).append("\n");
        sb.append("Progress: ").append(completedTasks).append("/").append(allTasks.size()).append(" tasks complete\n");
        sb.append("=".repeat(60)).append("\n\n");

        sb.append(formatTaskHierarchy(plan.rootTasks(), 0));

        return sb.toString();
    }

    private String formatTaskHierarchy(List<PlanStore.Task> tasks, int indent) {
        StringBuilder sb = new StringBuilder();
        String indentStr = "  ".repeat(indent);

        for (PlanStore.Task task : tasks) {
            sb.append(formatTask(task, indentStr));

            if (!task.subtasks().isEmpty()) {
                sb.append(formatTaskHierarchy(task.subtasks(), indent + 1));
            }
        }

        return sb.toString();
    }

    private String formatTask(PlanStore.Task task, String indent) {
        String statusIcon = switch (task.status()) {
            case PENDING -> "⏳";
            case IN_PROGRESS -> "🔄";
            case COMPLETED -> "✅";
            case FAILED -> "❌";
            case BLOCKED -> "🚫";
        };

        String executionBadge = task.executionType() == PlanStore.TaskExecutionType.PARALLEL ? " [PARALLEL]" : "";
        String deps = task.dependencies() != null && !task.dependencies().isEmpty() 
            ? " (deps: " + String.join(", ", task.dependencies()) + ")" 
            : "";

        return String.format("%s%s %s%s%s%n%s",
            indent,
            statusIcon,
            task.title(),
            executionBadge,
            deps,
            task.description() != null && !task.description().isBlank()
                ? indent + "  " + task.description() + "\n"
                : "");
    }

    private PlanStore.Task parseTaskFromJson(String json) {
        // Simplified JSON parsing for demonstration
        // In production, use proper JSON library
        try {
            // This is a placeholder - real implementation would parse JSON properly
            String title = json.contains("\"title\"") ? "Parsed Task" : "Task";
            return new PlanStore.Task(
                null,
                title,
                "",
                PlanStore.TaskStatus.PENDING,
                PlanStore.TaskExecutionType.SEQUENTIAL,
                new ArrayList<>(),
                new ArrayList<>(),
                java.time.Instant.now(),
                java.time.Instant.now()
            );
        } catch (Exception e) {
            throw new PlannerException("Invalid task JSON: " + json, e);
        }
    }
}
