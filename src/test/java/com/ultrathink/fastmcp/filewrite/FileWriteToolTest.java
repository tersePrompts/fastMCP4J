package com.ultrathink.fastmcp.filewrite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileWriteToolTest {

    private FileWriteTool tool;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        tool = new FileWriteTool();
    }

    @Test
    void testWriteFile() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";

        FileWriteResult result = tool.writeFile(testFile.toString(), content, false);

        assertNotNull(result);
        assertEquals(testFile.toString(), result.getPath());
        assertTrue(result.getBytesWritten() > 0);
        assertEquals(1, result.getLinesWritten());
        assertEquals("write", result.getOperation());
        assertTrue(result.isCreated());

        // Verify file was written
        String fileContent = Files.readString(testFile);
        assertEquals(content, fileContent);
    }

    @Test
    void testWriteFileOverwrite() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Original content");

        String newContent = "New content";
        FileWriteResult result = tool.writeFile(testFile.toString(), newContent, false);

        assertNotNull(result);
        assertFalse(result.isCreated()); // File already existed

        // Verify file was overwritten
        String fileContent = Files.readString(testFile);
        assertEquals(newContent, fileContent);
    }

    @Test
    void testWriteFileWithParentCreation() {
        Path testFile = tempDir.resolve("subdir/test.txt");

        String content = "Hello!";
        FileWriteResult result = tool.writeFile(testFile.toString(), content, true);

        assertNotNull(result);
        assertTrue(Files.exists(testFile));
        assertTrue(Files.exists(testFile.getParent()));
    }

    @Test
    void testWriteFileWithoutParentCreationFails() {
        Path testFile = tempDir.resolve("nonexistent/test.txt");

        String content = "Hello!";
        assertThrows(FileWriteException.class, () -> {
            tool.writeFile(testFile.toString(), content, false);
        });
    }

    @Test
    void testAppendFile() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Line 1\n");

        String appendContent = "Line 2\n";
        FileWriteResult result = tool.appendFile(testFile.toString(), appendContent, false);

        assertNotNull(result);
        assertEquals("append", result.getOperation());

        // Verify content was appended
        String fileContent = Files.readString(testFile);
        assertEquals("Line 1\nLine 2\n", fileContent);
    }

    @Test
    void testAppendFileCreateIfMissing() {
        Path testFile = tempDir.resolve("new.txt");

        String content = "New file content";
        FileWriteResult result = tool.appendFile(testFile.toString(), content, true);

        assertNotNull(result);
        assertTrue(result.isCreated());
        assertTrue(Files.exists(testFile));
    }

    @Test
    void testAppendFileFailsIfMissing() {
        Path testFile = tempDir.resolve("missing.txt");

        assertThrows(FileWriteException.class, () -> {
            tool.appendFile(testFile.toString(), "content", false);
        });
    }

    @Test
    void testWriteLines() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        List<String> lines = List.of("Line 1", "Line 2", "Line 3");

        FileWriteResult result = tool.writeLines(testFile.toString(), lines, false);

        assertNotNull(result);
        assertEquals(3, result.getLinesWritten());

        // Verify lines were written
        List<String> fileLines = Files.readAllLines(testFile);
        assertEquals(lines, fileLines);
    }

    @Test
    void testAppendLines() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Line 1\n");

        List<String> newLines = List.of("Line 2", "Line 3");
        FileWriteResult result = tool.appendLines(testFile.toString(), newLines, false);

        assertNotNull(result);

        // Verify lines were appended
        List<String> fileLines = Files.readAllLines(testFile);
        assertEquals(3, fileLines.size());
        assertEquals("Line 1", fileLines.get(0));
        assertEquals("Line 2", fileLines.get(1));
        assertEquals("Line 3", fileLines.get(2));
    }

    @Test
    void testDeleteFile() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "content");

        String result = tool.deleteFile(testFile.toString());

        assertNotNull(result);
        assertTrue(result.contains("Deleted"));
        assertFalse(Files.exists(testFile));
    }

    @Test
    void testDeleteFileNotExists() {
        Path testFile = tempDir.resolve("missing.txt");

        assertThrows(FileWriteException.class, () -> {
            tool.deleteFile(testFile.toString());
        });
    }

    @Test
    void testDeleteDirectory() throws IOException {
        Path testDir = tempDir.resolve("subdir");
        Files.createDirectory(testDir);

        assertThrows(FileWriteException.class, () -> {
            tool.deleteFile(testDir.toString());
        });
    }

    @Test
    void testCreateDirectory() {
        Path testDir = tempDir.resolve("newdir");

        String result = tool.createDirectory(testDir.toString(), false);

        assertNotNull(result);
        assertTrue(result.contains("Created"));
        assertTrue(Files.isDirectory(testDir));
    }

    @Test
    void testCreateDirectoryWithParents() {
        Path testDir = tempDir.resolve("parent/child/grandchild");

        String result = tool.createDirectory(testDir.toString(), true);

        assertNotNull(result);
        assertTrue(Files.isDirectory(testDir));
        assertTrue(Files.isDirectory(testDir.getParent()));
    }

    @Test
    void testCreateDirectoryAlreadyExists() throws IOException {
        Path testDir = tempDir.resolve("existing");
        Files.createDirectory(testDir);

        String result = tool.createDirectory(testDir.toString(), false);

        assertNotNull(result);
        assertTrue(result.contains("already exists"));
    }

    @Test
    void testDirectoryTraversalPrevention() {
        assertThrows(FileWriteException.class, () -> {
            tool.writeFile("../../../etc/passwd", "hacked", false);
        });
    }

    @Test
    void testFileSizeLimit() {
        // Create content larger than 10MB
        String largeContent = "x".repeat(11 * 1024 * 1024);
        Path testFile = tempDir.resolve("large.txt");

        assertThrows(FileWriteException.class, () -> {
            tool.writeFile(testFile.toString(), largeContent, false);
        });
    }

    @Test
    void testEmptyPathValidation() {
        assertThrows(FileWriteException.class, () -> {
            tool.writeFile("", "content", false);
        });

        assertThrows(FileWriteException.class, () -> {
            tool.writeFile(null, "content", false);
        });
    }

    @Test
    void testMultilineContent() throws IOException {
        Path testFile = tempDir.resolve("multiline.txt");
        String content = "Line 1\nLine 2\nLine 3";

        FileWriteResult result = tool.writeFile(testFile.toString(), content, false);

        assertEquals(3, result.getLinesWritten());

        String fileContent = Files.readString(testFile);
        assertEquals(content, fileContent);
    }
}
