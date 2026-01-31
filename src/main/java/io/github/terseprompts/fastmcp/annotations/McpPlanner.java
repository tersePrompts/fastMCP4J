package io.github.terseprompts.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable Planner tool — AI breaks complex tasks into steps.
 * <p>
 * AI can:
 * <ul>
 *   <li>Create hierarchical plans</li>
 *   <li>Add tasks and subtasks</li>
 *   <li>Track execution progress</li>
 *   <li>Manage task dependencies</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * @McpServer(name = "MyServer")
 * @McpPlanner  // AI plans
 * public class MyServer {
 *     public static void main(String[] args) {
 *         FastMCP.server(MyServer.class).run();
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpPlanner {
}
