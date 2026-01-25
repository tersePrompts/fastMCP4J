package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.McpMemory;
import com.ultrathink.fastmcp.annotations.McpParam;
import com.ultrathink.fastmcp.annotations.McpServer;
import com.ultrathink.fastmcp.annotations.McpTool;
import com.ultrathink.fastmcp.core.FastMCP;

/**
 * Example server demonstrating the Memory tool feature.
 * <p>
 * The @McpMemory annotation enables a memory tool that allows the AI to
 * persist and retrieve information across sessions.
 */
@McpServer(name = "MemoryExample", version = "1.0.0")
@McpMemory  // Enable memory tool
public class MemoryExampleServer {

    @McpTool(description = "Remember a piece of information")
    public String remember(
        @McpParam(description = "The information to remember")
        String info
    ) {
        // This tool can be called by the AI to remember things
        return "I'll remember: " + info;
    }

    @McpTool(description = "Process a task and save progress to memory")
    public String processTask(
        @McpParam(description = "Task name")
        String taskName,
        @McpParam(description = "Task details")
        String details
    ) {
        // The AI can use the memory tool to save progress
        return "Processing task: " + taskName;
    }

    public static void main(String[] args) {
        // Default: uses in-memory storage
        FastMCP.server(MemoryExampleServer.class)
            .stdio()
            .run();

        // OR with custom memory store:
        // FastMCP.server(MemoryExampleServer.class)
        //     .memoryStore(new InMemoryMemoryStore())
        //     .stdio()
        //     .run();
    }
}
