package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable File Write tool — AI writes and creates files.
 * <p>
 * AI gets:
 * <ul>
 *   <li>write_file — Create/overwrite</li>
 *   <li>append_file — Append content</li>
 *   <li>write_lines — Bulk write</li>
 *   <li>delete_file — Remove files</li>
 *   <li>create_directory — Make dirs</li>
 * </ul>
 * <p>
 * Path-validated, size-limited (10MB), line-limited (100k).
 * <p>
 * Example:
 * <pre>{@code
 * @McpServer(name = "MyServer")
 * @McpFileWrite  // AI writes files
 * public class MyServer {
 *     public static void main(String[] args) {
 *         FastMCP.server(MyServer.class).run();
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpFileWrite {
}
