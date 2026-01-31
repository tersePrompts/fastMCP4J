package io.github.terseprompts.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable File Read tool — AI reads your codebase.
 * <p>
 * AI gets:
 * <ul>
 *   <li>read_lines — Read ranges</li>
 *   <li>read_file — Full file</li>
 *   <li>grep — Search patterns</li>
 *   <li>file_stats — Metadata</li>
 * </ul>
 * <p>
 * Read-only, path-validated, size-limited.
 * <p>
 * Example:
 * <pre>{@code
 * @McpServer(name = "MyServer")
 * @McpFileRead  // AI reads files
 * public class MyServer {
 *     public static void main(String[] args) {
 *         FastMCP.server(MyServer.class).run();
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpFileRead {
}
