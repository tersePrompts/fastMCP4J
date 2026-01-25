package com.ultrathink.fastmcp.mcptools.planner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory plan store using concurrent collections.
 * <p>
 * Supports hierarchical task decomposition with:
 * - Sequential and parallel task execution
 * - Task dependencies
 * - Plan-level status tracking
 */
public class InMemoryPlanStore implements PlanStore {

    private final Map<String, Plan> plansById;
    private final Map<String, Task> tasksById;
    private final List<Plan> allPlans;

    public InMemoryPlanStore() {
        this.plansById = new ConcurrentHashMap<>();
        this.tasksById = new ConcurrentHashMap<>();
        this.allPlans = new CopyOnWriteArrayList<>();
    }

    @Override
    public String createPlan(String name, String description, List<Task> rootTasks) {
        String planId = generateId();
        Instant now = Instant.now();

        // Clear any existing tasks from previous plan creation with same tasks
        // Generate IDs for all tasks and store them
        List<Task> tasksWithIds = new ArrayList<>();
        for (Task task : rootTasks) {
            Task taskWithId = assignTaskIds(task);
            tasksWithIds.add(taskWithId);
        }

        // Ensure all tasks (including subtasks) are in tasksById
        for (Task task : tasksWithIds) {
            storeTaskRecursively(task);
        }

        Plan plan = new Plan(
            planId,
            name,
            description,
            TaskStatus.PENDING,
            now,
            now,
            tasksWithIds
        );

        plansById.put(planId, plan);
        allPlans.add(plan);

        return planId;
    }

    @Override
    public Plan getPlan(String planId) {
        return plansById.get(planId);
    }

    @Override
    public List<Plan> listPlans() {
        return new ArrayList<>(allPlans);
    }

    @Override
    public String addTask(String planId, Task task) {
        Plan plan = plansById.get(planId);
        if (plan == null) {
            throw new PlannerException("Plan not found: " + planId);
        }

        Task taskWithId = assignTaskIds(task);
        
        // Add new task to root tasks
        List<Task> newRootTasks = new ArrayList<>(plan.rootTasks());
        newRootTasks.add(taskWithId);

        Plan updated = new Plan(
            plan.id(),
            plan.name(),
            plan.description(),
            plan.status(),
            plan.createdAt(),
            Instant.now(),
            newRootTasks
        );

        plansById.put(planId, updated);
        allPlans.removeIf(p -> p.id().equals(planId));
        allPlans.add(updated);

        return taskWithId.id();
    }

    @Override
    public String addSubtask(String planId, String parentTaskId, Task subtask) {
        Task parent = tasksById.get(parentTaskId);
        if (parent == null) {
            throw new PlannerException("Parent task not found: " + parentTaskId);
        }

        Task subtaskWithId = assignTaskIds(subtask);

        // Add subtask to parent
        List<Task> newSubtasks = new ArrayList<>(parent.subtasks());
        newSubtasks.add(subtaskWithId);

        Task updatedParent = new Task(
            parent.id(),
            parent.title(),
            parent.description(),
            parent.status(),
            parent.executionType(),
            parent.dependencies(),
            newSubtasks,
            parent.createdAt(),
            Instant.now()
        );

tasksById.put(parentTaskId, updatedParent);
        
        // Also update the parent task in the plan's root tasks if it's there
        Plan plan = plansById.get(planId);
        if (plan != null) {
            List<Task> updatedRootTasks = updateTaskInList(plan.rootTasks(), updatedParent);
            Plan updatedPlan = new Plan(
                plan.id(),
                plan.name(),
                plan.description(),
                plan.status(),
                plan.createdAt(),
                Instant.now(),
                updatedRootTasks
            );
            plansById.put(planId, updatedPlan);
            allPlans.removeIf(p -> p.id().equals(planId));
            allPlans.add(updatedPlan);
        }

        return subtaskWithId.id();
    }

    @Override
    public void updateTaskStatus(String planId, String taskId, TaskStatus status) {
        Task task = tasksById.get(taskId);
        if (task == null) {
            throw new PlannerException("Task not found: " + taskId);
        }

        Task updated = new Task(
            task.id(),
            task.title(),
            task.description(),
            status,
            task.executionType(),
            task.dependencies(),
            task.subtasks(),
            task.createdAt(),
            Instant.now()
        );

tasksById.put(taskId, updated);
        
        // Also update the task in the plan's root tasks
        Plan plan = plansById.get(planId);
        if (plan != null) {
            List<Task> updatedRootTasks = updateTaskInList(plan.rootTasks(), updated);
            Plan updatedPlan = new Plan(
                plan.id(),
                plan.name(),
                plan.description(),
                plan.status(),
                plan.createdAt(),
                Instant.now(),
                updatedRootTasks
            );
            plansById.put(planId, updatedPlan);
            allPlans.removeIf(p -> p.id().equals(planId));
            allPlans.add(updatedPlan);
        }
    }

    @Override
    public List<Task> getAllTasks(String planId) {
        Plan plan = plansById.get(planId);
        if (plan == null) {
            throw new PlannerException("Plan not found: " + planId);
        }

        List<Task> allTasks = new ArrayList<>();
        flattenTasks(plan.rootTasks(), allTasks);
        return allTasks;
    }

@Override
    public Task getNextTask(String planId) {
        List<Task> allTasks = getAllTasks(planId);
        
        // Find first pending task with all dependencies satisfied
        for (Task task : allTasks) {
            if (task.status() == TaskStatus.PENDING && areDependenciesComplete(task)) {
                return task;
            }
        }
        
        return null;
    }

    @Override
    public void deletePlan(String planId) {
        Plan plan = plansById.remove(planId);
        if (plan == null) {
            throw new PlannerException("Plan not found: " + planId);
        }

        // Remove all tasks in the plan
        List<Task> allTasks = new ArrayList<>();
        flattenTasks(plan.rootTasks(), allTasks);
        allTasks.forEach(task -> tasksById.remove(task.id()));

        // Remove from list
        allPlans.removeIf(p -> p.id().equals(planId));
    }

    private Task assignTaskIds(Task task) {
        String taskId = generateId();
        List<Task> subtasksWithIds = new ArrayList<>();
        
        for (Task subtask : task.subtasks()) {
            subtasksWithIds.add(assignTaskIds(subtask));
        }

        Task taskWithId = new Task(
            taskId,
            task.title(),
            task.description(),
            task.status(),
            task.executionType(),
            task.dependencies(),
            subtasksWithIds,
            task.createdAt(),
            task.updatedAt()
        );

        tasksById.put(taskId, taskWithId);
        return taskWithId;
    }

    private void flattenTasks(List<Task> tasks, List<Task> result) {
        for (Task task : tasks) {
            result.add(task);
            flattenTasks(task.subtasks(), result);
        }
    }

    private void storeTaskRecursively(Task task) {
        tasksById.put(task.id(), task);
        for (Task subtask : task.subtasks()) {
            storeTaskRecursively(subtask);
        }
    }

    private boolean areDependenciesComplete(Task task) {
        if (task.dependencies() == null || task.dependencies().isEmpty()) {
            return true;
        }

        return task.dependencies().stream()
            .allMatch(depId -> {
                Task dep = tasksById.get(depId);
                return dep != null && dep.status() == TaskStatus.COMPLETED;
            });
    }

    private List<Task> updateTaskInList(List<Task> tasks, Task updatedTask) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (task.id().equals(updatedTask.id())) {
                result.add(updatedTask);
            } else {
                // Recursively update in subtasks
                List<Task> updatedSubtasks = updateTaskInList(task.subtasks(), updatedTask);
                if (!updatedSubtasks.equals(task.subtasks())) {
                    result.add(new Task(
                        task.id(),
                        task.title(),
                        task.description(),
                        task.status(),
                        task.executionType(),
                        task.dependencies(),
                        updatedSubtasks,
                        task.createdAt(),
                        task.updatedAt()
                    ));
                } else {
                    result.add(task);
                }
            }
        }
        return result;
    }

    private void updatePlanTimestamp(String planId) {
        Plan plan = plansById.get(planId);
        if (plan == null) {
            return;
        }

        Plan updated = new Plan(
            plan.id(),
            plan.name(),
            plan.description(),
            plan.status(),
            plan.createdAt(),
            Instant.now(),
            plan.rootTasks()
        );

        plansById.put(planId, updated);
        allPlans.removeIf(p -> p.id().equals(planId));
        allPlans.add(updated);
    }

    private String generateId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
