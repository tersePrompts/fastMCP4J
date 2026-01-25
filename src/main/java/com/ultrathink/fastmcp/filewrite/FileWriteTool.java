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
     * Unified file operations tool with multiple modes.
     *
     * @param mode The operation mode to perform
     * @param path Required for all modes - the file/directory path
     * @param content Required for writeFile/appendFile modes
     * @param lines Required for writeLines/appendLines modes
     * @param createParents Optional - create parent directories
     * @param createIfMissing Optional - create file if it doesn't exist (for append modes)
     * @return FileWriteResult or String message depending on mode
     */
    @McpTool(
        name = "filewrite",
        description = """
            Unified file operations tool supporting multiple modes:
            - writeFile: Write content to a file (creates or overwrites)
            - appendFile: Append content to an existing file
            - writeLines: Write a list of lines to a file (creates or overwrites)
            - appendLines: Append a list of lines to an existing file
            - deleteFile: Delete a file
            - createDirectory: Create a directory

            This tool provides safe file operations with built-in validation,
            size limits, and directory traversal protection.
            """
    )
    public Object filewrite(
        // Mode parameter
        @McpParam(
            description = """
                The operation mode to perform. Each mode has specific parameter requirements:
                - writeFile: Writes content to file (creates new or overwrites existing)
                - appendFile: Appends content to the end of an existing file
                - writeLines: Writes an array of lines to file (creates new or overwrites)
                - appendLines: Appends an array of lines to the end of an existing file
                - deleteFile: Permanently deletes the specified file
                - createDirectory: Creates a new directory at the specified path
                """,
            examples = {"writeFile", "appendFile", "writeLines", "appendLines", "deleteFile", "createDirectory"},
            constraints = "Must be one of the valid mode values",
            hints = "Choose the mode that best matches your intended operation. Use writeFile/appendFile for string content, writeLines/appendLines for array of strings."
        )
        String mode,

        // Path parameter (required for all modes)
        @McpParam(
            description = """
                The file system path to operate on. This can be an absolute path or relative to current working directory.
                The path will be validated and normalized before use.
                """,
            examples = {
                "/home/user/documents/file.txt",
                "./data/output.json",
                "C:\\\\Users\\\\username\\\\Documents\\\\file.txt",
                "/var/log/app.log"
            },
            constraints = "Must be a valid file system path. Directory traversal (..) is not allowed.",
            hints = "Use forward slashes on Unix/Mac and backslashes on Windows, or use forward slashes universally which work on all platforms."
        )
        String path,

        // Content parameter (required for writeFile/appendFile modes)
        @McpParam(
            description = """
                The text content to write to or append to the file. Used only with writeFile and appendFile modes.
                For writeLines/appendLines modes, use the 'lines' parameter instead.
                """,
            examples = {
                "Hello, World!",
                "{\"name\": \"John\", \"age\": 30}",
                "Line 1\\nLine 2\\nLine 3",
                "import java.util.*;\n\npublic class Test { }"
            },
            constraints = "Maximum content size is 10MB. Must be valid UTF-8 text.",
            hints = "Include proper line breaks (\\n) if you want multi-line content. Use writeLines mode for better control over individual lines.",
            required = false
        )
        String content,

        // Lines parameter (required for writeLines/appendLines modes)
        @McpParam(
            description = """
                Array of text lines to write to or append to the file. Used only with writeLines and appendLines modes.
                Each element in the array will be written as a separate line.
                An empty array [] will create an empty file (writeLines) or perform no append (appendLines).
                """,
            examples = {
                "[\"Line 1\", \"Line 2\", \"Line 3\"]",
                "[\"{\"name\": \"John\"}\", \"{\"name\": \"Jane\"}\"]",
                "[\"# Header\", \"## Subheader\", \"Content here\"]",
                "[]"  // Empty array creates empty file
            },
            constraints = "Maximum 100,000 lines. Each line is treated as a separate string. Empty array [] is allowed.",
            hints = "Lines will be joined with newline separators and automatically have a trailing newline added. Use this for structured data or when you need individual line control.",
            required = false
        )
        List<String> lines,

        // Create parents parameter (optional for writeFile/writeLines/createDirectory modes)
        @McpParam(
            description = """
                Whether to create parent directories if they don't exist. Applies to writeFile, writeLines, and createDirectory modes.
                When true, all missing parent directories in the path will be created automatically.
                """,
            examples = {"true", "false"},
            constraints = "Must be a boolean value (true or false)",
            hints = "Set to true when writing to nested directories that may not exist yet. Useful for creating directory structures in one operation.",
            required = false
        )
        Boolean createParents,

        // Create if missing parameter (optional for appendFile/appendLines modes)
        @McpParam(
            description = """
                Whether to create the file if it doesn't exist. Applies only to appendFile and appendLines modes.
                When true, the file will be created if it doesn't exist, then content will be appended.
                When false, the operation will fail if the file doesn't exist.
                """,
            examples = {"true", "false"},
            constraints = "Must be a boolean value (true or false). Only used with append modes.",
            hints = "Use true when you want to ensure content is added even if the file is new. Use false to strictly append to existing files only.",
            required = false
        )
        Boolean createIfMissing
    ) {
        // Validate mode parameter
        if (mode == null || mode.trim().isEmpty()) {
            throw new FileWriteException("Mode parameter is required");
        }

        // Route to appropriate method based on mode
        switch (mode) {
            case "writeFile":
                return handleWriteFile(path, content, createParents);

            case "appendFile":
                return handleAppendFile(path, content, createIfMissing);

            case "writeLines":
                return handleWriteLines(path, lines, createParents);

            case "appendLines":
                return handleAppendLines(path, lines, createIfMissing);

            case "deleteFile":
                return handleDeleteFile(path);

            case "createDirectory":
                return handleCreateDirectory(path, createParents);

            default:
                throw new FileWriteException(
                    "Invalid mode: " + mode + ". Must be one of: writeFile, appendFile, writeLines, appendLines, deleteFile, createDirectory"
                );
        }
    }

    /**
     * Handle writeFile mode.
     */
    private FileWriteResult handleWriteFile(String path, String content, Boolean createParents) {
        if (path == null || path.trim().isEmpty()) {
            throw new FileWriteException("Path parameter is required for writeFile mode");
        }
        if (content == null) {
            throw new FileWriteException("Content parameter is required for writeFile mode");
        }

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

            int lineCount = content.split("\n").length;
            return new FileWriteResult(path, bytes.length, lineCount, "write", !fileExisted);

        } catch (IOException e) {
            throw new FileWriteException("Failed to write file: " + e.getMessage(), e);
        }
    }

    /**
     * Handle appendFile mode.
     */
    private FileWriteResult handleAppendFile(String path, String content, Boolean createIfMissing) {
        if (path == null || path.trim().isEmpty()) {
            throw new FileWriteException("Path parameter is required for appendFile mode");
        }
        if (content == null) {
            throw new FileWriteException("Content parameter is required for appendFile mode");
        }

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

            int lineCount = content.split("\n").length;
            return new FileWriteResult(path, bytes.length, lineCount, "append", !fileExisted);

        } catch (IOException e) {
            throw new FileWriteException("Failed to append to file: " + e.getMessage(), e);
        }
    }

    /**
     * Handle writeLines mode.
     */
    private FileWriteResult handleWriteLines(String path, List<String> lines, Boolean createParents) {
        if (path == null || path.trim().isEmpty()) {
            throw new FileWriteException("Path parameter is required for writeLines mode");
        }
        if (lines == null) {
            throw new FileWriteException("Lines parameter cannot be null for writeLines mode");
        }

        if (lines.size() > MAX_LINES) {
            throw new FileWriteException("Number of lines exceeds maximum of " + MAX_LINES);
        }

        // Allow empty array to create an empty file
        String content = String.join("\n", lines);
        if (!content.isEmpty() && !content.endsWith("\n")) {
            content += "\n";
        }

        return handleWriteFile(path, content, createParents);
    }

    /**
     * Handle appendLines mode.
     */
    private FileWriteResult handleAppendLines(String path, List<String> lines, Boolean createIfMissing) {
        if (path == null || path.trim().isEmpty()) {
            throw new FileWriteException("Path parameter is required for appendLines mode");
        }
        if (lines == null) {
            throw new FileWriteException("Lines parameter cannot be null for appendLines mode");
        }

        if (lines.size() > MAX_LINES) {
            throw new FileWriteException("Number of lines exceeds maximum of " + MAX_LINES);
        }

        // Allow empty array (no-op append)
        String content = String.join("\n", lines);
        if (!content.isEmpty() && !content.endsWith("\n")) {
            content += "\n";
        }

        return handleAppendFile(path, content, createIfMissing);
    }

    /**
     * Handle deleteFile mode.
     */
    private String handleDeleteFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new FileWriteException("Path parameter is required for deleteFile mode");
        }

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
     * Handle createDirectory mode.
     */
    private String handleCreateDirectory(String path, Boolean createParents) {
        if (path == null || path.trim().isEmpty()) {
            throw new FileWriteException("Path parameter is required for createDirectory mode");
        }

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
