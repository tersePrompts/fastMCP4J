package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.McpServer;
import com.ultrathink.fastmcp.annotations.McpTool;
import com.ultrathink.fastmcp.annotations.McpTodo;
import com.ultrathink.fastmcp.core.FastMCP;
import com.ultrathink.fastmcp.annotations.McpParam;

/**
 * Example MCP server demonstrating Todo tool integration.
 * <p>
 * Use @McpTodo annotation to enable todo management capabilities.
 * The Todo tool provides task tracking with status management (pending, in_progress, completed).
 */
@McpServer(name = "Todo Example Server", version = "1.0.0")
@McpTodo
public class TodoExampleServer {

    /**
     * Custom business tool - greet the user.
     */
    @McpTool(description = "Greet the user with a personalized message")
    public String greet(
        @McpParam(
            description = "The name of the person to greet",
            examples = {"Alice", "Bob", "Charlie"}
        )
        String name
    ) {
        return String.format("Hello, %s! Welcome to the Todo Example Server.", name);
    }

    /**
     * Custom business tool - get current time.
     */
    @McpTool(description = "Get the current date and time")
    public String getCurrentTime() {
        return java.time.LocalDateTime.now().toString();
    }

    public static void main(String[] args) {
        FastMCP.server(TodoExampleServer.class)
            .stdio()
            .run();
    }
}
