package com.ultrathink.fastmcp.mcptools.filewrite;

import com.ultrathink.fastmcp.mcptools.filewrite.FileWriteException;
import com.ultrathink.fastmcp.mcptools.filewrite.FileWriteResult;
import com.ultrathink.fastmcp.mcptools.filewrite.FileWriteTool;
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

        FileWriteResult result = (FileWriteResult) tool.filewrite("writeFile", testFile.toString(), content, null, false, null);

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
        FileWriteResult result = (FileWriteResult) tool.filewrite("writeFile", testFile.toString(), newContent, null, false, null);

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
        FileWriteResult result = (FileWriteResult) tool.filewrite("writeFile", testFile.toString(), content, null, true, null);

        assertNotNull(result);
        assertTrue(Files.exists(testFile));
        assertTrue(Files.exists(testFile.getParent()));
    }

    @Test
    void testWriteFileWithoutParentCreationFails() {
        Path testFile = tempDir.resolve("nonexistent/test.txt");

        String content = "Hello!";
        assertThrows(FileWriteException.class, () -> {
            tool.filewrite("writeFile", testFile.toString(), content, null, false, null);
        });
    }

    @Test
    void testAppendFile() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Line 1\n");

        String appendContent = "Line 2\n";
        FileWriteResult result = (FileWriteResult) tool.filewrite("appendFile", testFile.toString(), appendContent, null, null, false);

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
        FileWriteResult result = (FileWriteResult) tool.filewrite("appendFile", testFile.toString(), content, null, null, true);

        assertNotNull(result);
        assertTrue(result.isCreated());
        assertTrue(Files.exists(testFile));
    }

    @Test
    void testAppendFileFailsIfMissing() {
        Path testFile = tempDir.resolve("missing.txt");

        assertThrows(FileWriteException.class, () -> {
            tool.filewrite("appendFile", testFile.toString(), "content", null, null, false);
        });
    }

    @Test
    void testWriteLines() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        List<String> lines = List.of("Line 1", "Line 2", "Line 3");

        FileWriteResult result = (FileWriteResult) tool.filewrite("writeLines", testFile.toString(), null, lines, false, null);

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
        FileWriteResult result = (FileWriteResult) tool.filewrite("appendLines", testFile.toString(), null, newLines, null, false);

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

        String result = (String) tool.filewrite("deleteFile", testFile.toString(), null, null, null, null);

        assertNotNull(result);
        assertTrue(result.contains("Deleted"));
        assertFalse(Files.exists(testFile));
    }

    @Test
    void testDeleteFileNotExists() {
        Path testFile = tempDir.resolve("missing.txt");

        assertThrows(FileWriteException.class, () -> {
            tool.filewrite("deleteFile", testFile.toString(), null, null, null, null);
        });
    }

    @Test
    void testDeleteDirectory() throws IOException {
        Path testDir = tempDir.resolve("subdir");
        Files.createDirectory(testDir);

        assertThrows(FileWriteException.class, () -> {
            tool.filewrite("deleteFile", testDir.toString(), null, null, null, null);
        });
    }

    @Test
    void testCreateDirectory() {
        Path testDir = tempDir.resolve("newdir");

        String result = (String) tool.filewrite("createDirectory", testDir.toString(), null, null, false, null);

        assertNotNull(result);
        assertTrue(result.contains("Created"));
        assertTrue(Files.isDirectory(testDir));
    }

    @Test
    void testCreateDirectoryWithParents() {
        Path testDir = tempDir.resolve("parent/child/grandchild");

        String result = (String) tool.filewrite("createDirectory", testDir.toString(), null, null, true, null);

        assertNotNull(result);
        assertTrue(Files.isDirectory(testDir));
        assertTrue(Files.isDirectory(testDir.getParent()));
    }

    @Test
    void testCreateDirectoryAlreadyExists() throws IOException {
        Path testDir = tempDir.resolve("existing");
        Files.createDirectory(testDir);

        String result = (String) tool.filewrite("createDirectory", testDir.toString(), null, null, false, null);

        assertNotNull(result);
        assertTrue(result.contains("already exists"));
    }

    @Test
    void testDirectoryTraversalPrevention() {
        assertThrows(FileWriteException.class, () -> {
            tool.filewrite("writeFile", "../../../etc/passwd", "hacked", null, false, null);
        });
    }

    @Test
    void testFileSizeLimit() {
        // Create content larger than 10MB
        String largeContent = "x".repeat(11 * 1024 * 1024);
        Path testFile = tempDir.resolve("large.txt");

        assertThrows(FileWriteException.class, () -> {
            tool.filewrite("writeFile", testFile.toString(), largeContent, null, false, null);
        });
    }

    @Test
    void testEmptyPathValidation() {
        assertThrows(FileWriteException.class, () -> {
            tool.filewrite("writeFile", "", "content", null, false, null);
        });

        assertThrows(FileWriteException.class, () -> {
            tool.filewrite("writeFile", null, "content", null, false, null);
        });
    }

    @Test
    void testMultilineContent() throws IOException {
        Path testFile = tempDir.resolve("multiline.txt");
        String content = "Line 1\nLine 2\nLine 3";

        FileWriteResult result = (FileWriteResult) tool.filewrite("writeFile", testFile.toString(), content, null, false, null);

        assertEquals(3, result.getLinesWritten());

        String fileContent = Files.readString(testFile);
        assertEquals(content, fileContent);
    }
}
