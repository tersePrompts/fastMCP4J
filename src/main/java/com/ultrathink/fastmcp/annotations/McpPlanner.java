package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable Planner tool for MCP server.
 * <p>
 * When present on a class annotated with {@link McpServer}, Planner tool
 * will be automatically registered. The tool provides task decomposition and
 * planning capabilities including:
 * - Creating hierarchical plans
 * - Adding tasks and subtasks
 * - Tracking execution progress
 * - Managing task dependencies
 * <p>
 * The planner tool is based on research into AI agent planning patterns:
 * - Task decomposition for complex problems [^1]
 * - Hierarchical planning with dependencies [^2]
 * - Memory-aided planning for context [^3]
 * - As-needed decomposition (ADaPT) [^4]
 * <p>
 * Example:
 * <pre>{@code
 * @McpServer(name = "MyServer")
 * @McpPlanner
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
 *
 * [^1]: https://arxiv.org/pdf/2402.02716 (Task decomposition frameworks)
 * [^2]: https://www.linkedin.com/pulse/task-decomposition-autonomous-ai-agents-principles-andre-9nmee
 * [^3]: https://www.analyticsvidhya.com/blog/2024/11/agentic-ai-planning-pattern
 * [^4]: https://aclanthology.org/2024.findings-naacl.264 (As-Needed Decomposition and Planning)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface McpPlanner {
}
