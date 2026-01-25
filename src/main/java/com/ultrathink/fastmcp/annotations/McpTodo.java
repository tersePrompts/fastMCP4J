package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable Todo tool for the MCP server.
 * <p>
 * When present on a class annotated with {@link McpServer}, the Todo tool
 * will be automatically registered. The tool provides task management capabilities
 * including adding, listing, updating, and deleting todos.
 * <p>
 * Example:
 * <pre>{@code
 * @McpServer(name = "MyServer")
 * @McpTodo
 * public class MyServer {
 *     // Custom business tools
 *     
 *     public static void main(String[] args) {
 *         FastMCP.server(MyServer.class)
 *             .stdio()
 *             .run();
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpTodo {
}
