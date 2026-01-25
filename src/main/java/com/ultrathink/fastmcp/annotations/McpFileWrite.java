package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables file writing capabilities for the MCP server.
 *
 * When this annotation is present on a server class, the following tools are registered:
 * - write_file: Write content to a file (create or overwrite)
 * - append_file: Append content to a file
 * - write_lines: Write lines to a file
 * - append_lines: Append lines to a file
 * - delete_file: Delete a file
 * - create_directory: Create a directory
 *
 * Security features:
 * - Path validation to prevent directory traversal
 * - File size limits (10MB default)
 * - Line count limits (100,000 default)
 * - Parent directory creation control
 *
 * Example:
 * <pre>
 * {@code
 * @McpServer(name = "MyServer")
 * @McpFileWrite
 * public class MyServer {
 *     // Your tools here
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpFileWrite {
}
