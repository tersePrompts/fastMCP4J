package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.context.Context;
import com.ultrathink.fastmcp.context.McpContext;
import com.ultrathink.fastmcp.core.FastMCP;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Comprehensive demo server showcasing FastMCP4J features:
 *
 * ANNOTATIONS:
 * - @McpServer: Server metadata with icons
 * - @McpTool: Tools with icons
 * - @McpResource: Resources with icons
 * - @McpPrompt: Prompts with icons
 * - @McpContext: Context injection
 * - @McpPreHook/@McpPostHook: Pre/post hooks
 * - @McpMemory: Memory tool
 * - @McpTodo: Todo tool
 * - @McpPlanner: Planner tool
 * - @McpFileRead: File reading tools
 * - @McpFileWrite: File writing tools
 * <p>
 * FEATURES:
 * - Enhanced parameter descriptions (@McpParam)
 * - Icons support (server, tools, resources, prompts)
 * - Pre/post execution hooks
 * - Multiple transport types (stdio, sse, streamable)
 * - Annotation-based tool enablement
 *
 * For async operations demo, see AsyncEcho.java
 */
@McpServer(
    name = "echo",
    version = "3.0.0",
    instructions = "Comprehensive FastMCP4J demo server with all features enabled",
    icons = {
        "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjY0IiBoZWlnaHQ9IjY0IiByeD0iMTIiIGZpbGw9IiM0Qzc5RkYiLz4KPHBhdGggZD0iTTMyIDE2QzIzLjE2MzQgMTYgMTYgMjMuMTYzNCAxNiAzMkMxNiA0MC44MzY2IDIzLjE2MzQgNDggMzIgNDhDMzguNjQ1IDQ4IDQ0LjM0MjIgNDQuMTM1NiA0Ny4xNDI4IDM4LjgzNDRMNTQuMjg1NiA0Ni45NzkyQzUwLjQxMTQgNTQuODUzMiA0MS44MzYgNjAgMzIgNjBDMjAuOTU0MyA2MCAxMiA1MS4wNDU3IDEyIDQwQzEyIDMzLjM2NDkgMTUuODY0NCAyNy42NTc4IDIxLjE2NTYgMjQuODU3MkwxMy4wMjA4IDE3LjcxNDlDMTYuODk1IDkuODM2MDkgMjUuNDcwNSA0IDMyIDRDNDMuMDQ1NyA0IDUyIDEyLjk1NDMgNTIgMjRDNTIgMzAuNjM1MSA0OC4xMzU2IDM2LjM0MjIgNDIuODM0NCAzOS4xNDI4TDUwLjk3OTIgNDYuOTc5MkM0Ny4xMDUgNTQuODU3MSAzOC41Mjk1IDYwIDMyIDYwQzIwLjk1NDMgNjAgMTIgNTEuMDQ1NyAxMiA0MEMxMiAzMy4zNjQ5IDE1Ljg2NDQgMjcuNjU3OCAyMS4xNjU2IDI0Ljg1NzJMMzIgMTZaIiBmaWxsPSJ3aGl0ZSIvPgo8L3N2Zz4=:image/svg+xml:64x64:light",
        "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjY0IiBoZWlnaHQ9IjY0IiByeD0iMTIiIGZpbGw9IiMxRTI5M0IiLz4KPHBhdGggZD0iTTMyIDE2QzIzLjE2MzQgMTYgMTYgMjMuMTYzNCAxNiAzMkMxNiA0MC44MzY2IDIzLjE2MzQgNDggMzIgNDhDMzguNjQ1IDQ4IDQ0LjM0MjIgNDQuMTM1NiA0Ny4xNDI4IDM4LjgzNDRMNTQuMjg1NiA0Ni45NzkyQzUwLjQxMTQgNTQuODUzMiA0MS44MzYgNjAgMzIgNjBDMjAuOTU0MyA2MCAxMiA1MS4wNDU3IDEyIDQwQzEyIDMzLjM2NDkgMTUuODY0NCAyNy42NTc4IDIxLjE2NTYgMjQuODU3MkwxMy4wMjA4IDE3LjcxNDlDMTYuODk1IDkuODM2MDkgMjUuNDcwNSA0IDMyIDRDNDMuMDQ1NyA0IDUyIDEyLjk1NDMgNTIgMjRDNTIgMzAuNjM1MSA0OC4xMzU2IDM2LjM0MjIgNDIuODM0NCAzOS4xNDI4TDUwLjk3OTIgNDYuOTc5MkM0Ny4xMDUgNTQuODU3MSAzOC41Mjk1IDYwIDMyIDYwQzIwLjk1NDMgNjAgMTIgNTEuMDQ1NyAxMiA0MEMxMiAzMy4zNjQ5IDE1Ljg2NDQgMjcuNjU3OCAyMS4xNjU2IDI0Ljg1NzJMMzIgMTZaIiBmaWxsPSJ3aGl0ZSIvPgo8L3N2Zz4=:image/svg+xml:64x64:dark"
    }
)
@McpMemory      // Enables: memory tool for persistent storage
@McpTodo        // Enables: todo_* tools for task management
@McpPlanner     // Enables: planner_* tools for planning
@McpFileRead    // Enables: read_lines, grep, file_stats tools
@McpFileWrite   // Enables: write_file, append_file, delete_file, create_directory tools
public class EchoServer {

    /**
     * Arithmetic operation types.
     */
    @Getter
    public enum Operation {
        ADD("add", "Addition (+)"),
        SUBTRACT("subtract", "Subtraction (-)"),
        MULTIPLY("multiply", "Multiplication (*)"),
        DIVIDE("divide", "Division (/)");

        private final String value;
        private final String description;

        Operation(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public static Operation fromString(String value) {
            for (Operation op : Operation.values()) {
                if (op.value.equalsIgnoreCase(value)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Invalid operation: " + value + ". Must be one of: add, subtract, multiply, divide");
        }
    }


    // ============================================
    // PRE/POST HOOKS - Logging & Monitoring
    // ============================================

    @McpPreHook(toolName = "*", order = 1)
    public void globalPreHook(Map<String, Object> args) {
        System.out.println("[HOOK] ⚡ Pre-execution: " + args);
    }

    @McpPostHook(toolName = "*", order = 1)
    public void globalPostHook(Map<String, Object> args, Object result) {
        System.out.println("[HOOK] ✅ Post-execution: " + result);
    }


    // ============================================
    // TOOLS - Basic Examples with Icons
    // ============================================

    @McpTool(
        description = "Echo back the message with timestamp and request headers",
        icons = {"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjMyIiBoZWlnaHQ9IjMyIiByeD0iOCIgZmlsbD0iIzg5REZGMyIvPgo8cGF0aCBkPSJNMTYgOEMxMi42ODY2IDggMTAgMTAuNjg2NiAxMCAxNEMxMCAxNy4zMTM0IDEyLjY4NjYgMjAgMTYgMjBDMTkuMzEzNCAyMCAyMiAxNy4zMTM0IDIyIDE0QzIyIDEwLjY4NjYgMTkuMzEzNCA4IDE2IDhaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTYgMTJDMTQuODk1NCAxMiAxNCAxMi44OTU0IDE0IDE0QzE0IDE1LjEwNDYgMTQuODk1NCAxNiAxNiAxNkMxNy4xMDQ2IDE2IDE4IDE1LjEwNDYgMTggMTRDMTggMTIuODk1NCAxNy4xMDQ2IDEyIDE2IDEyWiIgZmlsbD0iIzg5REZGMyIvPgo8L3N2Zz4=:image/svg+xml:32x32"}
    )
    public String echo(
        @McpParam(
            description = "Message to echo back",
            examples = {"Hello, World!", "Testing 123"},
            required = true
        )
        String message,
        @McpContext
        Context context
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] Echo: %s",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
            message));

        // Add truncated headers summary
        var headers = context.getHeaders();
        if (!headers.isEmpty()) {
            sb.append(String.format(" | Headers: %d", headers.size()));
            // Show first few keys, truncated
            headers.keySet().stream().limit(3).forEach(key -> {
                String val = headers.get(key);
                String truncated = val.length() > 20 ? val.substring(0, 17) + "..." : val;
                sb.append(String.format(" %s=%s", key, truncated));
            });
            if (headers.size() > 3) {
                sb.append(" +more");
            }
        }

        return sb.toString();
    }

    @McpTool(
        description = "Perform arithmetic calculations",
        icons = {"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjMyIiBoZWlnaHQ9IjMyIiByeD0iOCIgZmlsbD0iI0Y1OUEyMyIvPgo8cGF0aCBkPSJNMTAgMTBIMTRWMTRIMTBWMTBaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTggMThIMjJWMjJIMThWMThaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTAgMThIMTRWMjJIMTBWMThaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTggMTBIMjJWMTRIMThWMTRaIiBmaWxsPSJ3aGl0ZSIvPgo8L3N2Zz4=:image/svg+xml:32x32"}
    )
    public double calculate(
        @McpParam(
            description = "First number",
            examples = {"10", "25.5"},
            required = true
        )
        double a,

        @McpParam(
            description = "Operation to perform",
            examples = {"add", "subtract", "multiply", "divide"},
            constraints = "Must be one of: add, subtract, multiply, divide",
            hints = "Choose the arithmetic operation to perform on the two numbers",
            required = true
        )
        Operation operation,

        @McpParam(
            description = "Second number",
            examples = {"5", "12.3"},
            required = true
        )
        double b
    ) {
        return switch (operation) {
            case ADD -> a + b;
            case SUBTRACT -> a - b;
            case MULTIPLY -> a * b;
            case DIVIDE -> a / b;
        };
    }

    // ============================================
    // RESOURCES - Different Content Types with Icons
    // ============================================

    @McpResource(
        uri = "server://info",
        name = "Server Information",
        description = "Get current server status and information",
        mimeType = "text/plain",
        icons = {"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjMyIiBoZWlnaHQ9IjMyIiByeD0iOCIgZmlsbD0iIzY3Q0FDNSIvPgo8cGF0aCBkPSJNMTYgNkMxMC42ODYzIDYgNiAxMC42ODYzIDYgMTZDNiAyMS4zMTM3IDEwLjY4NjMgMjYgMTYgMjZDMjEuMzEzNyAyNiAyNiAyMS4zMTM3IDI2IDE2QzI2IDEwLjY4NjMgMjEuMzEzNyA2IDE2IDZaIiBmaWxsPSJ3aGl0ZSIvPgo8Y2lyY2xlIGN4PSIxNiIgY3k9IjE2IiByPSI0IiBmaWxsPSIjNjdDQUM1Ii8+Cjwvc3ZnPg==:image/svg+xml:32x32"}
    )
    public String getServerInfo() {
        return String.format("""
            ╔════════════════════════════════════════════╗
            ║     FastMCP4J Echo Server v3.0.0         ║
            ╚════════════════════════════════════════════╝

            Status: Running
            Started: %s

            ENABLED FEATURES:
            ✓ Memory Tool (@McpMemory)
            ✓ Todo Tool (@McpTodo)
            ✓ Planner Tool (@McpPlanner)
            ✓ FileRead Tool (@McpFileRead)
            ✓ FileWrite Tool (@McpFileWrite)
            ✓ Icons Support
            ✓ Pre/Post Hooks

            CUSTOM TOOLS:
            - echo: Echo messages with timestamp
            - calculate: Arithmetic operations

            BUILT-IN TOOLS:
            - memory: Persistent storage
            - todo: Task management
            - planner: Hierarchical planning
            - fileread: File reading operations
            - filewrite: File writing operations

            For async operations, see AsyncEcho.java
            """, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }



    public static void main(String[] args) {
        System.out.println("""
            ╔════════════════════════════════════════════════════════════╗
            ║     FastMCP4J Echo Server v3.0.0 - Core Features Demo    ║
            ╚════════════════════════════════════════════════════════════╝

            📦 Features Enabled:
               ✓ Memory Tool      (@McpMemory)
               ✓ Todo Tool        (@McpTodo)
               ✓ Planner Tool     (@McpPlanner)
               ✓ FileRead Tool    (@McpFileRead)
               ✓ FileWrite Tool   (@McpFileWrite)
               ✓ Icons Support    (server, tools, resources, prompts)
               ✓ Pre/Post Hooks   (@McpPreHook, @McpPostHook)

            🛠️  Custom Tools: 2 (echo, calculate)
            📄 Resources: 1
            💬 Prompts: 0

            🌐 Transport: HTTP Streamable
            🔌 Port: 3002
            📍 MCP URI: /mcp

            💡 For async operations, see AsyncEcho.java

            """);

        FastMCP.server(EchoServer.class)
            .streamable()
            // .sse()           // Alternative: SSE transport
            // .stdio()         // Alternative: STDIO transport
            .port(3002)
            .mcpUri("/mcp")
            .run();
    }
}
