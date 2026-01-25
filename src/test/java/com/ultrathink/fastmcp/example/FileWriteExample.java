package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.core.FastMCP;

/**
 * Example demonstrating file write capabilities.
 *
 * This server provides file writing tools when @McpFileWrite is present:
 * - write_file: Write content to a file (create or overwrite)
 * - append_file: Append content to a file
 * - write_lines: Write lines to a file
 * - append_lines: Append lines to a file
 * - delete_file: Delete a file
 * - create_directory: Create a directory
 *
 * Security features:
 * - Path validation (no directory traversal)
 * - File size limits (10MB)
 * - Line count limits (100,000)
 * - Parent directory creation control
 */
@McpServer(name = "FileWriteExample", version = "1.0.0")
@McpFileWrite
public class FileWriteExample {

    @McpTool(description = "Generate and save a report")
    public String generateReport(String filename, String title) {
        // This tool can use the file write tools internally
        // or clients can use the registered file write tools directly
        return "Report '" + title + "' would be saved to " + filename;
    }

    @McpTool(description = "Process and save data")
    public String processData(String inputPath, String outputPath) {
        return "Processing from " + inputPath + " to " + outputPath;
    }

    public static void main(String[] args) {
        FastMCP.server(FileWriteExample.class)
            .stdio()
            .run();
    }
}
