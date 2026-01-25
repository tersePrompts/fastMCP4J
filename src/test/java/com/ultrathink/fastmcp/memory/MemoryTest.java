package com.ultrathink.fastmcp.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemoryTest {

    private MemoryStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryMemoryStore();
    }

    @Test
    void testCreateAndReadFile() throws MemoryException {
        String path = "test.txt";
        String content = "Hello, World!";

        store.create(path, content);

        assertTrue(store.exists(path));

        MemoryStore.FileContent fileContent = store.read(path, Optional.empty());
        assertEquals(path, fileContent.path());
        assertEquals(1, fileContent.lines().size());
        assertEquals("Hello, World!", fileContent.lines().get(0));
    }

    @Test
    void testCreateFileAlreadyExists() {
        String path = "test.txt";
        String content = "Hello, World!";

        assertDoesNotThrow(() -> store.create(path, content));
        MemoryException ex = assertThrows(MemoryException.class, () -> store.create(path, content));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void testReadNonExistentFile() {
        MemoryException ex = assertThrows(MemoryException.class, () -> store.read("nonexistent.txt", Optional.empty()));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void testReplaceText() throws MemoryException {
        String path = "test.txt";
        String content = "Hello, World!";
        String oldText = "World";
        String newText = "Universe";

        store.create(path, content);
        store.replace(path, oldText, newText);

        MemoryStore.FileContent fileContent = store.read(path, Optional.empty());
        assertEquals("Hello, Universe!", fileContent.lines().get(0));
    }

    @Test
    void testReplaceTextNotFound() throws MemoryException {
        String path = "test.txt";
        String content = "Hello, World!";

        store.create(path, content);

        MemoryException ex = assertThrows(MemoryException.class,
            () -> store.replace(path, "Mars", "Universe"));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void testReplaceTextMultipleOccurrences() throws MemoryException {
        String path = "test.txt";
        String content = "Hello World, hello World";

        store.create(path, content);

        MemoryException ex = assertThrows(MemoryException.class,
            () -> store.replace(path, "World", "Universe"));
        assertTrue(ex.getMessage().contains("Multiple occurrences"));
    }

    @Test
    void testInsertText() throws MemoryException {
        String path = "test.txt";
        String content = "Line 1\nLine 3";

        store.create(path, content);
        store.insert(path, 1, "Line 2\n");

        MemoryStore.FileContent fileContent = store.read(path, Optional.empty());
        assertEquals(3, fileContent.lines().size());
        assertEquals("Line 1", fileContent.lines().get(0));
        assertEquals("Line 2", fileContent.lines().get(1));
        assertEquals("Line 3", fileContent.lines().get(2));
    }

    @Test
    void testInsertTextAtBeginning() throws MemoryException {
        String path = "test.txt";
        String content = "Line 2";

        store.create(path, content);
        store.insert(path, 0, "Line 1\n");

        MemoryStore.FileContent fileContent = store.read(path, Optional.empty());
        assertEquals(2, fileContent.lines().size());
        assertEquals("Line 1", fileContent.lines().get(0));
        assertEquals("Line 2", fileContent.lines().get(1));
    }

    @Test
    void testInsertTextAtEnd() throws MemoryException {
        String path = "test.txt";
        String content = "Line 1";

        store.create(path, content);
        store.insert(path, 1, "Line 2\n");

        MemoryStore.FileContent fileContent = store.read(path, Optional.empty());
        assertEquals(2, fileContent.lines().size());
        assertEquals("Line 1", fileContent.lines().get(0));
        assertEquals("Line 2", fileContent.lines().get(1));
    }

    @Test
    void testDeleteFile() throws MemoryException {
        String path = "test.txt";
        String content = "Hello, World!";

        store.create(path, content);
        assertTrue(store.exists(path));

        store.delete(path);
        assertFalse(store.exists(path));
    }

    @Test
    void testDeleteNonExistentFile() {
        MemoryException ex = assertThrows(MemoryException.class, () -> store.delete("nonexistent.txt"));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void testRenameFile() throws MemoryException {
        String oldPath = "old.txt";
        String newPath = "new.txt";
        String content = "Hello, World!";

        store.create(oldPath, content);
        store.rename(oldPath, newPath);

        assertFalse(store.exists(oldPath));
        assertTrue(store.exists(newPath));

        MemoryStore.FileContent fileContent = store.read(newPath, Optional.empty());
        assertEquals("Hello, World!", fileContent.lines().get(0));
    }

    @Test
    void testRenameToExistingFile() throws MemoryException {
        String path1 = "file1.txt";
        String path2 = "file2.txt";

        store.create(path1, "Content 1");
        store.create(path2, "Content 2");

        MemoryException ex = assertThrows(MemoryException.class, () -> store.rename(path1, path2));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void testListEmpty() throws MemoryException {
        List<MemoryStore.MemoryEntry> entries = store.list("");
        assertTrue(entries.isEmpty());
    }

    @Test
    void testListWithFiles() throws MemoryException {
        store.create("file1.txt", "Content 1");
        store.create("file2.txt", "Content 2");

        List<MemoryStore.MemoryEntry> entries = store.list("");
        assertEquals(2, entries.size());

        MemoryStore.MemoryEntry entry1 = entries.get(0);
        assertEquals("file1.txt", entry1.path());
        assertFalse(entry1.isDirectory());

        MemoryStore.MemoryEntry entry2 = entries.get(1);
        assertEquals("file2.txt", entry2.path());
        assertFalse(entry2.isDirectory());
    }

    @Test
    void testListWithSubdirectories() throws MemoryException {
        store.create("docs/README.md", "# Readme");
        store.create("docs/guide.md", "# Guide");
        store.create("src/Main.java", "class Main {}");

        List<MemoryStore.MemoryEntry> entries = store.list("");
        assertEquals(2, entries.size());

        MemoryStore.MemoryEntry docsEntry = entries.get(0);
        assertEquals("docs", docsEntry.path());
        assertTrue(docsEntry.isDirectory());

        MemoryStore.MemoryEntry srcEntry = entries.get(1);
        assertEquals("src", srcEntry.path());
        assertTrue(srcEntry.isDirectory());
    }

    @Test
    void testReadWithViewRange() throws MemoryException {
        String path = "test.txt";
        String content = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5";

        store.create(path, content);

        MemoryStore.FileContent fileContent = store.read(path, Optional.of(new int[]{2, 4}));
        assertEquals(path, fileContent.path());
        assertEquals(3, fileContent.lineNumbersToContent().size());
        assertEquals("Line 2", fileContent.lineNumbersToContent().get(2));
        assertEquals("Line 3", fileContent.lineNumbersToContent().get(3));
        assertEquals("Line 4", fileContent.lineNumbersToContent().get(4));
    }

    @Test
    void testGetMetadata() throws MemoryException {
        String path = "test.txt";
        String content = "Hello, World!";

        store.create(path, content);

        Optional<MemoryStore.MemoryEntry> metadata = store.getMetadata(path);
        assertTrue(metadata.isPresent());

        MemoryStore.MemoryEntry entry = metadata.get();
        assertEquals(path, entry.path());
        assertFalse(entry.isDirectory());
        assertTrue(entry.size() > 0);
        assertTrue(entry.lastModified() > 0);
    }

    @Test
    void testClear() throws MemoryException {
        store.create("file1.txt", "Content 1");
        store.create("file2.txt", "Content 2");

        assertTrue(store.exists("file1.txt"));
        assertTrue(store.exists("file2.txt"));

        store.clear();

        assertFalse(store.exists("file1.txt"));
        assertFalse(store.exists("file2.txt"));
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        String path = "concurrent.txt";
        int threadCount = 10;

        // Create threads that will all try to create different files
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    String filePath = "file" + index + ".txt";
                    store.create(filePath, "Content " + index);
                } catch (MemoryException e) {
                    // Expected if file already exists
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all files were created
        for (int i = 0; i < threadCount; i++) {
            assertTrue(store.exists("file" + i + ".txt"));
        }
    }
}
