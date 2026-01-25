package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables file reading capabilities for the MCP server.
 *
 * When this annotation is present on a server class, the following tools are registered:
 * - read_lines: Read a range of lines from a file
 * - read_file: Read entire file content
 * - grep: Search for pattern in files
 * - file_stats: Get file metadata (size, modified date, etc.)
 *
 * Security features:
 * - No file writing operations
 * - Path validation to prevent directory traversal
 * - File size limits
 * - Read-only operations
 *
 * Example:
 * <pre>
 * {@code
 * @McpServer(name = "MyServer")
 * @McpFileRead
 * public class MyServer {
 *     // Your tools here
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpFileRead {
}
