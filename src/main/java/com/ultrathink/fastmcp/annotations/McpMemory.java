package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable Memory tool — AI remembers across sessions.
 * <p>
 * AI gets persistent storage for:
 * <ul>
 *   <li>User preferences</li>
 *   <li>Project context</li>
 *   <li>Past decisions</li>
 *   <li>Learned information</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * @McpServer(name = "MyServer")
 * @McpMemory  // AI remembers
 * public class MyServer {
 *     public static void main(String[] args) {
 *         FastMCP.server(MyServer.class).run();
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpMemory {
}
