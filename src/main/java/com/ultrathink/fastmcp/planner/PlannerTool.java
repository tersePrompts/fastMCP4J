package com.ultrathink.fastmcp.planner;

import com.ultrathink.fastmcp.annotations.McpParam;
import com.ultrathink.fastmcp.annotations.McpTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     * Unified planner tool for managing execution plans and tasks.
     * <p>
     * This single tool provides all planning operations through the mode parameter.
     * Each mode has specific parameters that are only relevant for that operation.
     */
    @McpTool(description = """
        Unified planner tool for managing execution plans and hierarchical tasks.
        Supports creating plans, adding tasks/subtasks, updating status, tracking progress,
        and managing complex multi-step workflows with dependencies.

        Available modes:
        - createPlan: Create a new plan with optional initial tasks
        - listPlans: List all available plans with status summaries
        - getPlan: Get detailed view of a specific plan with full task hierarchy
        - addTask: Add a new root-level task to an existing plan
        - addSubtask: Add a subtask to break down a task into smaller steps
        - updateTask: Update the status of a task (pending, in_progress, completed, failed, blocked)
        - getNextTask: Get the next pending task that can be executed (all dependencies complete)
        - deletePlan: Delete a plan and all its tasks
        """)
    public String planner(
        // Mode parameter (always required)
        @McpParam(
            description = """
                Operation mode to perform. Each mode has specific parameters:

                1. createPlan - Creates a new execution plan for decomposing complex tasks
                2. listPlans - Lists all available plans with status and progress
                3. getPlan - Retrieves detailed plan information with full task hierarchy
                4. addTask - Adds a new task to an existing plan
                5. addSubtask - Adds a subtask to break down a parent task
                6. updateTask - Updates the status of an existing task
                7. getNextTask - Gets the next executable pending task
                8. deletePlan - Deletes a plan and all its tasks
                """,
            examples = {"createPlan", "addTask", "updateTask", "getNextTask"},
            constraints = "Must be one of: createPlan, listPlans, getPlan, addTask, addSubtask, updateTask, getNextTask, deletePlan",
            hints = "Choose the mode that best matches your current objective. For example: use 'createPlan' to start a new project, 'addTask' to add steps, 'getNextTask' to get work, 'updateTask' to mark progress."
        )
        String mode,

        // Common parameters (planId, taskId) - used by multiple modes
        @McpParam(
            description = """
                Plan identifier - required for: getPlan, addTask, addSubtask, updateTask, getNextTask, deletePlan.
                Get this from the createPlan response or listPlans output.
                """,
            examples = {"abc12345-def67-89012", "plan-2024-rest-api", "xyz98765"},
            constraints = "Must be a valid plan ID that exists in the system",
            hints = "Copy the plan ID from the output of createPlan() or listPlans()",
            required = false
        )
        String planId,

        @McpParam(
            description = """
                Task identifier - required for: addSubtask, updateTask.
                Get this from the addTask response or getPlan output.
                """,
            examples = {"task-001", "task-auth-setup", "abc-def-123"},
            constraints = "Must be a valid task ID that exists within the specified plan",
            hints = "Copy the task ID from the output of addTask() or getPlan()",
            required = false
        )
        String taskId,

        // createPlan mode parameters
        @McpParam(
            description = """
                [createPlan mode] Plan name/title - short descriptive name for the plan.
                Should clearly indicate what the plan accomplishes.
                """,
            examples = {"Build REST API", "Migrate database to MongoDB", "Implement authentication system", "Deploy microservices to K8s"},
            constraints = "Cannot be empty or null. Should be concise but descriptive (2-10 words recommended)",
            hints = "Use a clear, actionable name that describes the overall goal. Example: 'Implement user authentication with JWT' rather than 'Auth'",
            required = false
        )
        String planName,

        @McpParam(
            description = """
                [createPlan mode] Detailed description of the plan's objectives and context.
                Explain what the plan aims to accomplish and any important background.
                """,
            examples = {
                "Implement user management with CRUD operations, password hashing, and JWT authentication",
                "Migrate user data from PostgreSQL to MongoDB including schema transformation and data validation",
                "Create a complete REST API for product catalog with search, filtering, and pagination"
            },
            constraints = "Should explain the goal, scope, and context. 1-5 sentences recommended",
            hints = "Provide enough context for someone to understand what success looks like. Include technical details, constraints, or requirements if relevant",
            required = false
        )
        String planDescription,

        @McpParam(
            description = """
                [createPlan mode] Initial root-level tasks as a list of task objects.
                Each task must have 'title', and may include 'description', 'execution_type' ('sequential' or 'parallel'), and 'dependencies' (array of task IDs).

                Example: [{"title": "Setup database", "description": "Configure PostgreSQL"}, {"title": "Build API", "execution_type": "parallel"}]
                """,
            examples = {
                "[{\"title\": \"Setup project structure\", \"description\": \"Create directories and config files\"}]",
                "[{\"title\": \"Design schema\", \"description\": \"Plan database tables\"}, {\"title\": \"Implement models\", \"description\": \"Create data models\", \"execution_type\": \"sequential\"}]",
                "[{\"title\": \"Setup frontend\", \"execution_type\": \"parallel\"}, {\"title\": \"Setup backend\", \"execution_type\": \"parallel\"}]"
            },
            constraints = "Must be an array of task objects. Each task must have at least a 'title' field",
            hints = "Start with 3-7 major tasks. You can always add more tasks later with addTask mode. Break down complex tasks into subtasks later using addSubtask mode",
            required = false
        )
        List<Map<String, Object>> initialTasks,

        // addTask mode parameters
        @McpParam(
            description = """
                [addTask mode] Task title - a short, clear description of what the task does.
                Should be action-oriented and specific.
                """,
            examples = {"Setup PostgreSQL database", "Write unit tests for API", "Deploy to staging environment", "Configure authentication middleware"},
            constraints = "Cannot be empty. Should be 2-10 words. Use verb-noun format (e.g., 'Implement user login')",
            hints = "Be specific enough that someone knows what to do. 'Fix bug' is too vague; 'Fix authentication token expiration bug' is better",
            required = false
        )
        String taskTitle,

        @McpParam(
            description = """
                [addTask mode] Detailed task description with context, requirements, and acceptance criteria.
                """,
            examples = {
                "Configure PostgreSQL connection pool with max 20 connections, 5 min idle, and 30 second timeout",
                "Write comprehensive unit tests for all REST API endpoints covering success and error cases",
                "Deploy the application to staging environment using Docker Compose and verify health checks"
            },
            constraints = "Should provide enough detail to execute the task. 1-5 sentences recommended",
            hints = "Include technical details, requirements, file paths, or acceptance criteria. The more specific, the better",
            required = false
        )
        String taskDescription,

        @McpParam(
            description = """
                [addTask mode] Execution type determining if this task's subtasks run sequentially or in parallel.
                - sequential: Subtasks run one after another (default, safer)
                - parallel: Subtasks can run simultaneously (faster, requires independence)
                """,
            examples = {"sequential", "parallel"},
            constraints = "Must be either 'sequential' or 'parallel'. Default is 'sequential'",
            hints = "Use 'parallel' only when subtasks are independent and can run simultaneously without conflicts. Use 'sequential' when order matters or there are dependencies",
            required = false
        )
        String executionType,

        @McpParam(
            description = """
                [addTask mode] List of task IDs that must complete before this task can start.
                Used to create task dependencies and enforce execution order.
                """,
            examples = {"[\"task-001\", \"task-002\"]", "[\"setup-db\"]", "[]"},
            constraints = "All task IDs must exist in the same plan. Cannot create circular dependencies",
            hints = "Only list direct dependencies. For example, if task C depends on B which depends on A, only list B in C's dependencies, not A. Get task IDs from getPlan() output",
            required = false
        )
        List<String> dependencies,

        // addSubtask mode parameters
        @McpParam(
            description = """
                [addSubtask mode] Parent task ID that this subtask belongs to.
                The subtask will be nested under this parent task in the hierarchy.
                """,
            examples = {"task-001", "task-db-setup", "parent-task-abc"},
            constraints = "Must be a valid task ID in the specified plan",
            hints = "Get the parent task ID from getPlan() output. Subtasks are used to break down complex tasks into manageable steps",
            required = false
        )
        String parentTaskId,

        @McpParam(
            description = """
                [addSubtask mode] Subtask title - a short, clear description of the subtask.
                Should represent a concrete step that contributes to completing the parent task.
                """,
            examples = {"Create database schema", "Write integration tests", "Verify deployment logs"},
            constraints = "Cannot be empty. Should be 2-10 words. More granular than parent task titles",
            hints = "Subtasks should be smaller and more focused than parent tasks. Each subtask should be completable in a focused work session",
            required = false
        )
        String subtaskTitle,

        @McpParam(
            description = """
                [addSubtask mode] Detailed subtask description with specific steps and requirements.
                """,
            examples = {
                "Create users table with id, email, password_hash columns and unique constraint on email",
                "Write tests for POST /users endpoint covering validation, authentication, and error handling",
                "Check Kubernetes pod logs for errors and verify all services are running"
            },
            constraints = "Should provide concrete steps or technical details. 1-3 sentences recommended",
            hints = "Be more specific than the parent task description. Include file names, function names, or exact commands when possible",
            required = false
        )
        String subtaskDescription,

        // updateTask mode parameters
        @McpParam(
            description = """
                [updateTask mode] New task status indicating current progress state.

                Status meanings:
                - pending: Task is not started yet (default)
                - in_progress: Currently being worked on
                - completed: Task finished successfully
                - failed: Task failed and needs attention
                - blocked: Task cannot proceed due to dependencies or issues
                """,
            examples = {"completed", "in_progress", "failed", "blocked", "pending"},
            constraints = "Must be one of: pending, in_progress, completed, failed, blocked",
            hints = "Update status regularly to track progress. Move tasks from pending → in_progress → completed. Use 'blocked' if waiting on something, 'failed' if errors occur",
            required = false
        )
        String status
    ) {
        // Route to the appropriate handler based on mode
        return switch (mode) {
            case "createPlan" -> handleCreatePlan(planName, planDescription, initialTasks);
            case "listPlans" -> handleListPlans();
            case "getPlan" -> handleGetPlan(planId);
            case "addTask" -> handleAddTask(planId, taskTitle, taskDescription, executionType, dependencies);
            case "addSubtask" -> handleAddSubtask(planId, parentTaskId, subtaskTitle, subtaskDescription);
            case "updateTask" -> handleUpdateTask(planId, taskId, status);
            case "getNextTask" -> handleGetNextTask(planId);
            case "deletePlan" -> handleDeletePlan(planId);
            default -> throw new PlannerException(
                "Invalid mode: " + mode + ". Valid modes are: createPlan, listPlans, getPlan, addTask, addSubtask, updateTask, getNextTask, deletePlan"
            );
        };
    }

    /**
     * Handle createPlan mode - creates a new execution plan.
     */
    private String handleCreatePlan(String name, String description, List<Map<String, Object>> initialTasks) {
        if (name == null || name.isBlank()) {
            throw new PlannerException("Plan name is required for createPlan mode. Provide planName parameter.");
        }

        List<PlanStore.Task> tasks = new ArrayList<>();
        if (initialTasks != null) {
            for (Map<String, Object> taskMap : initialTasks) {
                PlanStore.Task task = parseTaskFromMap(taskMap);
                tasks.add(task);
            }
        }

        String planId = store.createPlan(name, description, tasks);
        return String.format("Created plan '%s' (ID: %s) with %d root task(s)%n%s",
            name, planId, tasks.size(),
            tasks.isEmpty() ? "Use addTask mode to add tasks." : "");
    }

    /**
     * Handle listPlans mode - lists all available plans.
     */
    private String handleListPlans() {
        List<PlanStore.Plan> plans = store.listPlans();

        if (plans.isEmpty()) {
            return "No plans found. Use planner with createPlan mode to create a new plan.";
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
     * Handle getPlan mode - retrieves detailed plan information.
     */
    private String handleGetPlan(String planId) {
        if (planId == null || planId.isBlank()) {
            throw new PlannerException("Plan ID is required for getPlan mode. Provide planId parameter.");
        }

        PlanStore.Plan plan = store.getPlan(planId);
        if (plan == null) {
            throw new PlannerException("Plan not found: " + planId);
        }

        return formatPlanDetails(plan);
    }

    /**
     * Handle addTask mode - adds a new task to a plan.
     */
    private String handleAddTask(String planId, String title, String description, String executionType, List<String> dependencies) {
        if (planId == null || planId.isBlank()) {
            throw new PlannerException("Plan ID is required for addTask mode. Provide planId parameter.");
        }
        if (title == null || title.isBlank()) {
            throw new PlannerException("Task title is required for addTask mode. Provide taskTitle parameter.");
        }

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
     * Handle addSubtask mode - adds a subtask to a parent task.
     */
    private String handleAddSubtask(String planId, String parentTaskId, String title, String description) {
        if (planId == null || planId.isBlank()) {
            throw new PlannerException("Plan ID is required for addSubtask mode. Provide planId parameter.");
        }
        if (parentTaskId == null || parentTaskId.isBlank()) {
            throw new PlannerException("Parent task ID is required for addSubtask mode. Provide parentTaskId parameter.");
        }
        if (title == null || title.isBlank()) {
            throw new PlannerException("Subtask title is required for addSubtask mode. Provide subtaskTitle parameter.");
        }

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
     * Handle updateTask mode - updates task status.
     */
    private String handleUpdateTask(String planId, String taskId, String status) {
        if (planId == null || planId.isBlank()) {
            throw new PlannerException("Plan ID is required for updateTask mode. Provide planId parameter.");
        }
        if (taskId == null || taskId.isBlank()) {
            throw new PlannerException("Task ID is required for updateTask mode. Provide taskId parameter.");
        }
        if (status == null || status.isBlank()) {
            throw new PlannerException("Status is required for updateTask mode. Provide status parameter.");
        }

        PlanStore.TaskStatus newStatus = parseStatus(status);
        if (newStatus == null) {
            throw new PlannerException("Invalid status: " + status + ". Use: pending, in_progress, completed, failed, blocked");
        }

        store.updateTaskStatus(planId, taskId, newStatus);
        return String.format("Updated task %s status to %s", taskId, newStatus.toString().toLowerCase());
    }

    /**
     * Handle getNextTask mode - retrieves the next pending task to work on.
     */
    private String handleGetNextTask(String planId) {
        if (planId == null || planId.isBlank()) {
            throw new PlannerException("Plan ID is required for getNextTask mode. Provide planId parameter.");
        }

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
     * Handle deletePlan mode - deletes a plan and all its tasks.
     */
    private String handleDeletePlan(String planId) {
        if (planId == null || planId.isBlank()) {
            throw new PlannerException("Plan ID is required for deletePlan mode. Provide planId parameter.");
        }

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

    private PlanStore.Task parseTaskFromMap(Map<String, Object> taskMap) {
        try {
            // Extract title (required)
            Object titleObj = taskMap.get("title");
            if (titleObj == null) {
                throw new PlannerException("Task object must contain a 'title' field");
            }
            String title = titleObj.toString();
            if (title.isBlank()) {
                throw new PlannerException("Task 'title' cannot be empty");
            }

            // Extract description (optional)
            Object descObj = taskMap.get("description");
            String description = descObj != null ? descObj.toString() : "";

            // Extract execution_type (optional)
            Object execTypeObj = taskMap.get("execution_type");
            PlanStore.TaskExecutionType execType = PlanStore.TaskExecutionType.SEQUENTIAL;
            if (execTypeObj != null) {
                execType = parseExecutionType(execTypeObj.toString());
            }

            // Extract dependencies (optional)
            List<String> dependencies = new ArrayList<>();
            Object depsObj = taskMap.get("dependencies");
            if (depsObj instanceof List<?> depsList) {
                for (Object dep : depsList) {
                    if (dep != null) {
                        dependencies.add(dep.toString());
                    }
                }
            }

            return new PlanStore.Task(
                null,
                title,
                description,
                PlanStore.TaskStatus.PENDING,
                execType,
                dependencies,
                new ArrayList<>(),
                java.time.Instant.now(),
                java.time.Instant.now()
            );
        } catch (Exception e) {
            throw new PlannerException("Invalid task object: " + taskMap + ". Error: " + e.getMessage(), e);
        }
    }
}
