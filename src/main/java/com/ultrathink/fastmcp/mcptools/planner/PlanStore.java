package com.ultrathink.fastmcp.mcptools.planner;

import java.time.Instant;
import java.util.List;

/**
 * High-level interface for plan storage and execution.
 * <p>
 * Supports task decomposition, hierarchical planning, and plan tracking.
 */
public interface PlanStore {

    /**
     * Create a new plan.
     *
     * @param name plan name
     * @param description plan description
     * @param rootTasks root-level tasks
     * @return plan ID
     */
    String createPlan(String name, String description, List<Task> rootTasks);

    /**
     * Get a plan by ID.
     *
     * @param planId plan ID
     * @return the plan
     */
    Plan getPlan(String planId);

    /**
     * List all plans.
     *
     * @return list of plans
     */
    List<Plan> listPlans();

    /**
     * Add a task to a plan.
     *
     * @param planId plan ID
     * @param task the task to add
     * @return task ID
     */
    String addTask(String planId, Task task);

    /**
     * Add a subtask to an existing task.
     *
     * @param planId plan ID
     * @param parentTaskId parent task ID
     * @param subtask the subtask
     * @return subtask ID
     */
    String addSubtask(String planId, String parentTaskId, Task subtask);

    /**
     * Update task status.
     *
     * @param planId plan ID
     * @param taskId task ID
     * @param status new status
     */
    void updateTaskStatus(String planId, String taskId, TaskStatus status);

    /**
     * Get all tasks in a plan (flattened hierarchy).
     *
     * @param planId plan ID
     * @return list of all tasks
     */
    List<Task> getAllTasks(String planId);

    /**
     * Get the next pending task in execution order.
     *
     * @param planId plan ID
     * @return next task, or null if no pending tasks
     */
    Task getNextTask(String planId);

    /**
     * Delete a plan.
     *
     * @param planId plan ID
     */
    void deletePlan(String planId);

    /**
     * Plan record.
     */
    record Plan(
        String id,
        String name,
        String description,
        TaskStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<Task> rootTasks
    ) {}

    /**
     * Task record.
     */
    record Task(
        String id,
        String title,
        String description,
        TaskStatus status,
        TaskExecutionType executionType,
        List<String> dependencies,
        List<Task> subtasks,
        Instant createdAt,
        Instant updatedAt
    ) {}

    /**
     * Task status.
     */
    enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        BLOCKED
    }

    /**
     * Task execution type.
     */
    enum TaskExecutionType {
        /** Execute sequentially (one at a time) */
        SEQUENTIAL,
        /** Can execute in parallel with other tasks */
        PARALLEL
    }
}
