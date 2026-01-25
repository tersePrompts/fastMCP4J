package com.ultrathink.fastmcp.todo;

import com.ultrathink.fastmcp.mcptools.todo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InMemoryTodoStore.
 */
public class InMemoryTodoStoreTest {

    private TodoStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryTodoStore();
    }

    @Test
    void testAddTodo() {
        String id = store.add("First task");
        assertNotNull(id);
        
        TodoItem todo = store.get(id);
        assertEquals("First task", todo.task());
        assertEquals(TodoStatus.PENDING, todo.status());
    }

    @Test
    void testListAll() {
        store.add("Task 1");
        store.add("Task 2");
        store.add("Task 3");

        List<TodoItem> todos = store.list(null);
        assertEquals(3, todos.size());
    }

    @Test
    void testListByStatus() {
        String id1 = store.add("Task 1");
        String id2 = store.add("Task 2");
        String id3 = store.add("Task 3");

        store.updateStatus(id1, TodoStatus.IN_PROGRESS);
        store.updateStatus(id2, TodoStatus.COMPLETED);

        List<TodoItem> pending = store.list(TodoStatus.PENDING);
        assertEquals(1, pending.size());

        List<TodoItem> inProgress = store.list(TodoStatus.IN_PROGRESS);
        assertEquals(1, inProgress.size());

        List<TodoItem> completed = store.list(TodoStatus.COMPLETED);
        assertEquals(1, completed.size());
    }

    @Test
    void testUpdateStatus() {
        String id = store.add("Task 1");
        
        store.updateStatus(id, TodoStatus.IN_PROGRESS);
        TodoItem todo = store.get(id);
        assertEquals(TodoStatus.IN_PROGRESS, todo.status());

        store.updateStatus(id, TodoStatus.COMPLETED);
        todo = store.get(id);
        assertEquals(TodoStatus.COMPLETED, todo.status());
    }

    @Test
    void testDelete() {
        String id = store.add("Task 1");
        assertNotNull(store.get(id));

        store.delete(id);
        assertNull(store.get(id));
    }

    @Test
    void testClearCompleted() {
        store.add("Task 1");
        store.add("Task 2");
        store.add("Task 3");

        String id1 = store.list(TodoStatus.PENDING).get(0).id();
        store.updateStatus(id1, TodoStatus.IN_PROGRESS);

        String id2 = store.list(TodoStatus.PENDING).get(0).id();
        store.updateStatus(id2, TodoStatus.COMPLETED);

        assertEquals(3, store.count(null));
        assertEquals(1, store.count(TodoStatus.COMPLETED));

        store.clearCompleted();

        assertEquals(2, store.count(null));
        assertEquals(0, store.count(TodoStatus.COMPLETED));
        assertEquals(1, store.count(TodoStatus.IN_PROGRESS));
        assertEquals(1, store.count(TodoStatus.PENDING));
    }

    @Test
    void testCount() {
        store.add("Task 1");
        store.add("Task 2");
        store.add("Task 3");

        String id1 = store.list(TodoStatus.PENDING).get(0).id();
        store.updateStatus(id1, TodoStatus.IN_PROGRESS);

        String id2 = store.list(TodoStatus.PENDING).get(0).id();
        store.updateStatus(id2, TodoStatus.COMPLETED);

        assertEquals(3, store.count(null));
        assertEquals(1, store.count(TodoStatus.IN_PROGRESS));
        assertEquals(1, store.count(TodoStatus.COMPLETED));
        assertEquals(1, store.count(TodoStatus.PENDING));
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int tasksPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < tasksPerThread; j++) {
                    store.add("Task " + threadNum + "-" + j);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(threadCount * tasksPerThread, store.count(null));
    }

    @Test
    void testDeleteNonExistent() {
        assertThrows(TodoException.class, () -> store.delete("nonexistent-id"));
    }

    @Test
    void testUpdateNonExistent() {
        assertThrows(TodoException.class, () -> 
            store.updateStatus("nonexistent-id", TodoStatus.COMPLETED)
        );
    }
}
