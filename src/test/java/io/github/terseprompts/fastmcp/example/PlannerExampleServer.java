package io.github.terseprompts.fastmcp.example;

import io.github.terseprompts.fastmcp.annotations.McpPlanner;
import io.github.terseprompts.fastmcp.annotations.McpServer;
import io.github.terseprompts.fastmcp.annotations.McpTool;
import io.github.terseprompts.fastmcp.annotations.McpParam;
import io.github.terseprompts.fastmcp.core.FastMCP;

/**
 * Example MCP server demonstrating Planner tool integration.
 * <p>
 * Use @McpPlanner annotation to enable task decomposition and planning capabilities.
 * The Planner tool provides hierarchical task planning with dependencies and execution tracking.
 */
@McpServer(name = "Planner Example Server", version = "1.0.0")
@McpPlanner
public class PlannerExampleServer {

    /**
     * Custom business tool - generate code.
     */
    @McpTool(description = "Generate code for a given task")
    public String generateCode(
        @McpParam(
            description = "The code task description",
            examples = {"Create a REST API endpoint for user management", "Implement a sorting algorithm"}
        )
        String task
    ) {
        return String.format("Here's the code for: %s\n\n// Implementation would go here...", task);
    }

    /**
     * Custom business tool - run tests.
     */
    @McpTool(description = "Run tests for the project")
    public String runTests() {
        return "Running tests...\n\nAll tests passed! (simulated)";
    }

    /**
     * Custom business tool - deploy to production.
     */
    @McpTool(description = "Deploy application to production environment")
    public String deploy() {
        return "Deploying to production...\n\nDeployment successful! (simulated)";
    }

    public static void main(String[] args) {
        FastMCP.server(PlannerExampleServer.class)
            .stdio()
            .run();
    }
}
