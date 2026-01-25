package com.ultrathink.fastmcp.planner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InMemoryPlanStore.
 */
public class InMemoryPlanStoreTest {

    private PlanStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryPlanStore();
    }

    @Test
    void testCreatePlan() {
        String planId = store.createPlan("Test Plan", "Description", new ArrayList<>());
        assertNotNull(planId);

        PlanStore.Plan plan = store.getPlan(planId);
        assertEquals("Test Plan", plan.name());
        assertEquals("Description", plan.description());
        assertEquals(PlanStore.TaskStatus.PENDING, plan.status());
    }

    @Test
    void testListPlans() {
        store.createPlan("Plan 1", "Desc 1", new ArrayList<>());
        store.createPlan("Plan 2", "Desc 2", new ArrayList<>());

        List<PlanStore.Plan> plans = store.listPlans();
        assertEquals(2, plans.size());
    }

    @Test
    void testAddTask() {
        String planId = store.createPlan("Test Plan", "Description", new ArrayList<>());

        PlanStore.Task task = new PlanStore.Task(
            null, "Task 1", "Description",
            PlanStore.TaskStatus.PENDING,
            PlanStore.TaskExecutionType.SEQUENTIAL,
            new ArrayList<>(), new ArrayList<>(),
            Instant.now(), Instant.now()
        );

        String taskId = store.addTask(planId, task);
        assertNotNull(taskId);

        List<PlanStore.Task> tasks = store.getAllTasks(planId);
        assertEquals(1, tasks.size());
        assertEquals("Task 1", tasks.get(0).title());
    }

    @Test
    void testAddSubtask() {
        String planId = store.createPlan("Test Plan", "Description", new ArrayList<>());

        PlanStore.Task parentTask = new PlanStore.Task(
            null, "Parent", "Parent description",
            PlanStore.TaskStatus.PENDING,
            PlanStore.TaskExecutionType.SEQUENTIAL,
            new ArrayList<>(), new ArrayList<>(),
            Instant.now(), Instant.now()
        );
        String parentTaskId = store.addTask(planId, parentTask);

        PlanStore.Task subtask = new PlanStore.Task(
            null, "Subtask", "Subtask description",
            PlanStore.TaskStatus.PENDING,
            PlanStore.TaskExecutionType.SEQUENTIAL,
            new ArrayList<>(), new ArrayList<>(),
            Instant.now(), Instant.now()
        );
        String subtaskId = store.addSubtask(planId, parentTaskId, subtask);

        assertNotNull(subtaskId);

        List<PlanStore.Task> tasks = store.getAllTasks(planId);
        assertEquals(2, tasks.size());

        PlanStore.Task parent = tasks.stream()
            .filter(t -> t.id().equals(parentTaskId))
            .findFirst()
            .orElse(null);
        assertNotNull(parent);
        assertEquals(1, parent.subtasks().size());
    }

    @Test
    void testUpdateTaskStatus() {
        String planId = store.createPlan("Test Plan", "Description", new ArrayList<>());

        PlanStore.Task task = new PlanStore.Task(
            null, "Task 1", "Description",
            PlanStore.TaskStatus.PENDING,
            PlanStore.TaskExecutionType.SEQUENTIAL,
            new ArrayList<>(), new ArrayList<>(),
            Instant.now(), Instant.now()
        );
        String taskId = store.addTask(planId, task);

        store.updateTaskStatus(planId, taskId, PlanStore.TaskStatus.IN_PROGRESS);

        List<PlanStore.Task> tasks = store.getAllTasks(planId);
        PlanStore.Task updated = tasks.get(0);
        assertEquals(PlanStore.TaskStatus.IN_PROGRESS, updated.status());

        store.updateTaskStatus(planId, taskId, PlanStore.TaskStatus.COMPLETED);

        tasks = store.getAllTasks(planId);
        updated = tasks.get(0);
        assertEquals(PlanStore.TaskStatus.COMPLETED, updated.status());
    }

    @Test
    void testGetNextTask() {
        String planId = store.createPlan("Test Plan", "Description", new ArrayList<>());

        PlanStore.Task task1 = createTask("Task 1");
        store.addTask(planId, task1);

        PlanStore.Task task2 = createTask("Task 2");
        String task2Id = store.addTask(planId, task2);

        PlanStore.Task task3 = createTask("Task 3");
        PlanStore.Task task3WithDeps = new PlanStore.Task(
            null, task3.title(), task3.description(),
            PlanStore.TaskStatus.PENDING,
            PlanStore.TaskExecutionType.SEQUENTIAL,
            List.of(task2Id), // Depends on task2
            new ArrayList<>(),
            Instant.now(), Instant.now()
        );
        store.addTask(planId, task3WithDeps);

        // First available task should be task1 or task2 (no dependencies)
        PlanStore.Task next = store.getNextTask(planId);
        assertNotNull(next);
        assertTrue(next.id().equals(task1.id()) || next.id().equals(task2Id));

        // Mark task1 as in progress
        store.updateTaskStatus(planId, task1.id(), PlanStore.TaskStatus.IN_PROGRESS);

        // Next should be task2 (task3 depends on task2)
        next = store.getNextTask(planId);
        assertNotNull(next);
        assertEquals(task2Id, next.id());

        // Complete task2
        store.updateTaskStatus(planId, task2Id, PlanStore.TaskStatus.COMPLETED);

        // Now task3 should be available
        next = store.getNextTask(planId);
        assertNotNull(next);
        assertEquals(task3WithDeps.id(), next.id());
    }

    @Test
    void testDeletePlan() {
        String planId = store.createPlan("Test Plan", "Description", new ArrayList<>());

        PlanStore.Task task1 = createTask("Task 1");
        store.addTask(planId, task1);

        PlanStore.Task task2 = createTask("Task 2");
        store.addTask(planId, task2);

        assertEquals(1, store.listPlans().size());
        assertEquals(2, store.getAllTasks(planId).size());

        store.deletePlan(planId);

        assertNull(store.getPlan(planId));
        assertEquals(0, store.listPlans().size());
    }

    @Test
    void testDependencies() {
        String planId = store.createPlan("Test Plan", "Description", new ArrayList<>());

        PlanStore.Task task1 = createTask("Task 1");
        String task1Id = store.addTask(planId, task1);

        PlanStore.Task task2 = createTask("Task 2");
        String task2Id = store.addTask(planId, task2);

        String task3Id = null; // Declare first
        
        PlanStore.Task task3WithDeps = new PlanStore.Task(
            null, task3.title(), task3.description(),
            PlanStore.TaskStatus.PENDING,
            PlanStore.TaskExecutionType.SEQUENTIAL,
            List.of(task1Id, task3Id), // Depends on BOTH
            new ArrayList<>(),
            Instant.now(), Instant.now()
        );
        task3Id = store.addTask(planId, task3WithDeps);

        // Task3 should not be available until both task1 and task2 are done
        PlanStore.Task next = store.getNextTask(planId);
        assertNotNull(next);
        assertFalse(next.id().equals(task3Id));

        // Complete task1 only
        store.updateTaskStatus(planId, task1Id, PlanStore.TaskStatus.COMPLETED);

        next = store.getNextTask(planId);
        assertNotNull(next);
        assertFalse(next.id().equals(task3Id));

        // Complete task2
        store.updateTaskStatus(planId, task2Id, PlanStore.TaskStatus.COMPLETED);

        // Now task3 should be available
        next = store.getNextTask(planId);
        assertNotNull(next);
        assertEquals(task3Id, next.id());
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 5;
        int plansPerThread = 20;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < plansPerThread; j++) {
                    store.createPlan("Plan " + threadNum + "-" + j, "Test", new ArrayList<>());
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(threadCount * plansPerThread, store.listPlans().size());
    }

    private PlanStore.Task createTask(String title) {
        return new PlanStore.Task(
            null, title, "Description",
            PlanStore.TaskStatus.PENDING,
            PlanStore.TaskExecutionType.SEQUENTIAL,
            new ArrayList<>(), new ArrayList<>(),
            Instant.now(), Instant.now()
        );
    }

    @Test
    void testGetNonExistentPlan() {
        assertNull(store.getPlan("nonexistent-id"));
    }

    @Test
    void testDeleteNonExistentPlan() {
        assertThrows(PlannerException.class, () -> 
            store.deletePlan("nonexistent-id")
        );
    }

    @Test
    void testUpdateNonExistentTask() {
        String planId = store.createPlan("Test Plan", "Description", new ArrayList<>());
        assertThrows(PlannerException.class, () -> 
            store.updateTaskStatus(planId, "nonexistent-id", PlanStore.TaskStatus.COMPLETED)
        );
    }
}
