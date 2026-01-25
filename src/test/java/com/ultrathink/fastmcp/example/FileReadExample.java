package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.core.FastMCP;

/**
 * Example demonstrating file read capabilities.
 *
 * This server provides file reading tools when @McpFileRead is present:
 * - read_lines: Read a range of lines from a file
 * - read_file: Read entire file content
 * - grep: Search for pattern in files
 * - file_stats: Get file metadata
 *
 * Security features:
 * - Read-only operations (no file writing)
 * - Path validation (no directory traversal)
 * - File size limits
 */
@McpServer(name = "FileReadExample", version = "1.0.0")
@McpFileRead
public class FileReadExample {

    @McpTool(description = "Analyze log file for errors")
    public String analyzeLogs(String logPath) {
        // This tool can use the file read tools internally
        // or clients can use the registered file read tools directly
        return "Analyzing logs at " + logPath;
    }

    @McpTool(description = "Search configuration files")
    public String searchConfig(String pattern) {
        return "Searching for pattern: " + pattern;
    }

    public static void main(String[] args) {
        FastMCP.server(FileReadExample.class)
            .stdio()
            .run();
    }
}
