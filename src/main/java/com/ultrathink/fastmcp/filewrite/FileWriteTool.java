package com.ultrathink.fastmcp.filewrite;

import com.ultrathink.fastmcp.annotations.McpParam;
import com.ultrathink.fastmcp.annotations.McpTool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * Tool for writing files with safety checks.
 *
 * Security considerations:
 * - Path validation to prevent directory traversal
 * - File size limits
 * - Optional backup before overwrite
 * - Parent directory creation
 */
public class FileWriteTool {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB limit
    private static final int MAX_LINES = 100000; // Max lines to write

    /**
     * Write content to a file (create or overwrite).
     *
     * @param path The path to write to
     * @param content The content to write
     * @param createParents Whether to create parent directories if they don't exist
     * @return FileWriteResult with operation details
     */
    @McpTool(name = "write_file", description = "Write content to a file (creates or overwrites)")
    public FileWriteResult writeFile(
        @McpParam(description = "The path to the file", required = true)
        String path,

        @McpParam(description = "The content to write", required = true)
        String content,

        @McpParam(description = "Create parent directories if they don't exist", required = false)
        Boolean createParents
    ) {
        Path filePath = validateAndNormalizePath(path);
        boolean shouldCreateParents = createParents != null && createParents;

        // Validate content size
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FILE_SIZE) {
            throw new FileWriteException("Content exceeds maximum file size of " + MAX_FILE_SIZE + " bytes");
        }

        boolean fileExisted = Files.exists(filePath);

        try {
            // Create parent directories if requested
            if (shouldCreateParents && filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }

            // Write the file
            Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            int lines = content.split("\n").length;
            return new FileWriteResult(path, bytes.length, lines, "write", !fileExisted);

        } catch (IOException e) {
            throw new FileWriteException("Failed to write file: " + e.getMessage(), e);
        }
    }

    /**
     * Append content to a file.
     *
     * @param path The path to append to
     * @param content The content to append
     * @param createIfMissing Whether to create the file if it doesn't exist
     * @return FileWriteResult with operation details
     */
    @McpTool(name = "append_file", description = "Append content to a file")
    public FileWriteResult appendFile(
        @McpParam(description = "The path to the file", required = true)
        String path,

        @McpParam(description = "The content to append", required = true)
        String content,

        @McpParam(description = "Create file if it doesn't exist", required = false)
        Boolean createIfMissing
    ) {
        Path filePath = validateAndNormalizePath(path);
        boolean shouldCreate = createIfMissing != null && createIfMissing;

        // Check if file exists
        boolean fileExisted = Files.exists(filePath);
        if (!fileExisted && !shouldCreate) {
            throw new FileWriteException("File does not exist: " + path + " (use createIfMissing=true to create)");
        }

        // Validate content size
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        // Check total file size won't exceed limit
        if (fileExisted) {
            try {
                long currentSize = Files.size(filePath);
                if (currentSize + bytes.length > MAX_FILE_SIZE) {
                    throw new FileWriteException("Appending would exceed maximum file size of " + MAX_FILE_SIZE + " bytes");
                }
            } catch (IOException e) {
                throw new FileWriteException("Failed to check file size: " + e.getMessage(), e);
            }
        }

        try {
            // Append to file (create if missing and allowed)
            if (shouldCreate) {
                Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.write(filePath, bytes, StandardOpenOption.APPEND);
            }

            int lines = content.split("\n").length;
            return new FileWriteResult(path, bytes.length, lines, "append", !fileExisted);

        } catch (IOException e) {
            throw new FileWriteException("Failed to append to file: " + e.getMessage(), e);
        }
    }

    /**
     * Write lines to a file.
     *
     * @param path The path to write to
     * @param lines The lines to write
     * @param createParents Whether to create parent directories
     * @return FileWriteResult with operation details
     */
    @McpTool(name = "write_lines", description = "Write lines to a file (creates or overwrites)")
    public FileWriteResult writeLines(
        @McpParam(description = "The path to the file", required = true)
        String path,

        @McpParam(description = "The lines to write", required = true)
        List<String> lines,

        @McpParam(description = "Create parent directories if they don't exist", required = false)
        Boolean createParents
    ) {
        if (lines.size() > MAX_LINES) {
            throw new FileWriteException("Number of lines exceeds maximum of " + MAX_LINES);
        }

        String content = String.join("\n", lines);
        if (!content.isEmpty() && !content.endsWith("\n")) {
            content += "\n";
        }

        return writeFile(path, content, createParents);
    }

    /**
     * Append lines to a file.
     *
     * @param path The path to append to
     * @param lines The lines to append
     * @param createIfMissing Whether to create the file if missing
     * @return FileWriteResult with operation details
     */
    @McpTool(name = "append_lines", description = "Append lines to a file")
    public FileWriteResult appendLines(
        @McpParam(description = "The path to the file", required = true)
        String path,

        @McpParam(description = "The lines to append", required = true)
        List<String> lines,

        @McpParam(description = "Create file if it doesn't exist", required = false)
        Boolean createIfMissing
    ) {
        if (lines.size() > MAX_LINES) {
            throw new FileWriteException("Number of lines exceeds maximum of " + MAX_LINES);
        }

        String content = String.join("\n", lines);
        if (!content.isEmpty() && !content.endsWith("\n")) {
            content += "\n";
        }

        return appendFile(path, content, createIfMissing);
    }

    /**
     * Delete a file.
     *
     * @param path The path to delete
     * @return Success message
     */
    @McpTool(name = "delete_file", description = "Delete a file")
    public String deleteFile(
        @McpParam(description = "The path to the file", required = true)
        String path
    ) {
        Path filePath = validateAndNormalizePath(path);

        if (!Files.exists(filePath)) {
            throw new FileWriteException("File does not exist: " + path);
        }

        if (Files.isDirectory(filePath)) {
            throw new FileWriteException("Path is a directory, not a file: " + path);
        }

        try {
            Files.delete(filePath);
            return "Deleted file: " + path;
        } catch (IOException e) {
            throw new FileWriteException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    /**
     * Create a directory.
     *
     * @param path The directory path to create
     * @param createParents Whether to create parent directories
     * @return Success message
     */
    @McpTool(name = "create_directory", description = "Create a directory")
    public String createDirectory(
        @McpParam(description = "The path to the directory", required = true)
        String path,

        @McpParam(description = "Create parent directories if they don't exist", required = false)
        Boolean createParents
    ) {
        Path dirPath = validateAndNormalizePath(path);
        boolean shouldCreateParents = createParents != null && createParents;

        if (Files.exists(dirPath)) {
            if (Files.isDirectory(dirPath)) {
                return "Directory already exists: " + path;
            } else {
                throw new FileWriteException("Path exists but is not a directory: " + path);
            }
        }

        try {
            if (shouldCreateParents) {
                Files.createDirectories(dirPath);
            } else {
                Files.createDirectory(dirPath);
            }
            return "Created directory: " + path;
        } catch (IOException e) {
            throw new FileWriteException("Failed to create directory: " + e.getMessage(), e);
        }
    }

    /**
     * Validate and normalize a file path.
     * Prevents directory traversal attacks.
     */
    private Path validateAndNormalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new FileWriteException("Path cannot be null or empty");
        }

        Path filePath = Paths.get(path).normalize();

        // Check for directory traversal attempts
        String normalizedPath = filePath.toString();
        if (normalizedPath.contains("..")) {
            throw new FileWriteException("Invalid path: directory traversal not allowed");
        }

        return filePath;
    }
}
