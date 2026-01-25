package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable memory tool for a FastMCP server.
 * When present on a server class, a memory tool will be automatically registered.
 * <p>
 * The memory tool allows the AI to persist and retrieve information across sessions,
 * making it useful for maintaining context, learning from past interactions, and building
 * knowledge bases over time.
 * <p>
 * Example:
 * <pre>
 * {@code
 * @McpServer(name = "MyServer")
 * @McpMemory  // Enables memory tool
 * public class MyServer {
 *
 *     @McpTool
 *     public String myTool(String input) {
 *         // Can use context to access memory if needed
 *         return "Result";
 *     }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpMemory {
}
