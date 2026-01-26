package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable Todo tool — AI can track, update, and manage tasks.
 * <p>
 * Add this annotation to your {@link McpServer} class. AI gets:
 * <ul>
 *   <li>add — Create tasks with priority</li>
 *   <li>list — View all tasks</li>
 *   <li>updateStatus — Mark tasks done/in-progress</li>
 *   <li>updateTask — Edit task details</li>
 *   <li>delete — Remove tasks</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * @McpServer(name = "MyServer")
 * @McpTodo  // AI can now manage tasks
 * public class MyServer {
 *     public static void main(String[] args) {
 *         FastMCP.server(MyServer.class).run();
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpTodo {
}
