package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.context.Context;
import com.ultrathink.fastmcp.context.McpContext;
import com.ultrathink.fastmcp.core.FastMCP;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive demo server showcasing ALL FastMCP4J features:
 *
 * ANNOTATIONS:
 * - @McpServer: Server metadata with icons
 * - @McpTool: Tools with icons
 * - @McpResource: Resources with icons
 * - @McpPrompt: Prompts with icons
 * - @McpAsync: Async operations
 * - @McpContext: Context injection
 * - @McpPreHook/@McpPostHook: Pre/post hooks
 * - @McpMemory: Memory tool
 * - @McpTodo: Todo tool
 * - @McpPlanner: Planner tool
 * - @McpFileRead: File reading tools
 * - @McpFileWrite: File writing tools
 *
 * FEATURES:
 * - Enhanced parameter descriptions (@McpParam)
 * - Icons support (server, tools, resources, prompts)
 * - Context access (session state, notifications)
 * - Pre/post execution hooks
 * - Reactive programming (Mono/Flux)
 * - Multiple transport types (stdio, sse, streamable)
 * - Annotation-based tool enablement
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
        description = "Echo back the message with timestamp",
        icons = {"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjMyIiBoZWlnaHQ9IjMyIiByeD0iOCIgZmlsbD0iIzg5REZGMyIvPgo8cGF0aCBkPSJNMTYgOEMxMi42ODY2IDggMTAgMTAuNjg2NiAxMCAxNEMxMCAxNy4zMTM0IDEyLjY4NjYgMjAgMTYgMjBDMTkuMzEzNCAyMCAyMiAxNy4zMTM0IDIyIDE0QzIyIDEwLjY4NjYgMTkuMzEzNCA4IDE2IDhaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTYgMTJDMTQuODk1NCAxMiAxNCAxMi44OTU0IDE0IDE0QzE0IDE1LjEwNDYgMTQuODk1NCAxNiAxNiAxNkMxNy4xMDQ2IDE2IDE4IDE1LjEwNDYgMTggMTRDMTggMTIuODk1NCAxNy4xMDQ2IDEyIDE2IDEyWiIgZmlsbD0iIzg5REZGMyIvPgo8L3N2Zz4=:image/svg+xml:32x32"}
    )
    public String echo(
        @McpParam(
            description = "Message to echo back",
            examples = {"Hello, World!", "Testing 123"},
            required = true
        )
        String message
    ) {
        return String.format("[%s] Echo: %s",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
            message);
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
            required = true
        )
        String operation,

        @McpParam(
            description = "Second number",
            examples = {"5", "12.3"},
            required = true
        )
        double b
    ) {
        return switch (operation.toLowerCase()) {
            case "add" -> a + b;
            case "subtract" -> a - b;
            case "multiply" -> a * b;
            case "divide" -> a / b;
            default -> throw new IllegalArgumentException("Invalid operation: " + operation);
        };
    }


    @McpTool(
        description = "Process task with progress reporting using Context",
        icons = {"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjMyIiBoZWlnaHQ9IjMyIiByeD0iOCIgZmlsbD0iIzEwQjk4MSIvPgo8Y2lyY2xlIGN4PSIxNiIgY3k9IjE2IiByPSI4IiBzdHJva2U9IndoaXRlIiBzdHJva2Utd2lkdGg9IjIiIGZpbGw9Im5vbmUiLz4KPHBhdGggZD0iTTE2IDE2VjEyIiBzdHJva2U9IndoaXRlIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPgo8L3N2Zz4=:image/svg+xml:32x32"}
    )
    public String processWithProgress(
        @McpParam(description = "Number of steps", examples = {"10", "100"})
        int steps,

        @McpContext Context ctx
    ) {
        ctx.info("Starting process with " + steps + " steps");

        for (int i = 1; i <= steps; i++) {
            ctx.reportProgress(i, steps, "Processing step " + i + " of " + steps);
            try {
                Thread.sleep(10); // Simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        ctx.info("Process completed!");
        return "Completed " + steps + " steps";
    }

    // ============================================
    // TOOLS - Async Examples with Context
    // ============================================

    @McpTool(
        description = "Async task with progress reporting",
        icons = {"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjMyIiBoZWlnaHQ9IjMyIiByeD0iOCIgZmlsbD0iIzg0NDU5OSIvPgo8cGF0aCBkPSJNMTYgOEMxMS41ODE3IDggOCAxMS41ODE3IDggMTZDOCAyMC40MTgzIDExLjU4MTcgMjQgMTYgMjRDMjAuNDE4MyAyNCAyNCAyMC40MTgzIDI0IDE2QzI0IDExLjU4MTcgMjAuNDE4MyA4IDE2IDhaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTYgMTJWMjAiIHN0cm9rZT0iIzg0NDU5OSIgc3Ryb2tlLXdpZHRoPSIzIiBzdHJva2UtbGluZWNhcD0icm91bmQiLz4KPHAJdGggZD0iTTE2IDEyTDIwIDE2TDE2IDIwTDEyIDE2TDE2IDEyWiIgZmlsbD0iIzg0NDU5OSIvPgo8L3N2Zz4=:image/svg+xml:32x32"}
    )
    @McpAsync
    public Mono<String> asyncTask(
        @McpParam(
            description = "Task name to execute",
            examples = {"backup", "sync", "analyze"}
        )
        String taskName,

        @McpParam(
            description = "Duration in seconds",
            examples = {"5", "10"},
            defaultValue = "3",
            required = false
        )
        int durationSeconds,

        @McpContext Context ctx
    ) {
        return Mono.fromRunnable(() -> {
            ctx.info("🚀 Starting async task: " + taskName);
        })
        .then(Mono.defer(() -> {
            // Simulate work with progress
            for (int i = 1; i <= durationSeconds; i++) {
                ctx.reportProgress(i, durationSeconds, "Processing " + taskName + " (" + i + "/" + durationSeconds + ")");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ctx.info("✅ Completed async task: " + taskName);
            return Mono.just(String.format("Task '%s' completed in %d seconds at %s",
                taskName, durationSeconds, LocalDateTime.now()));
        }));
    }

    // ============================================
    // RESOURCES - Different Content Types with Icons
    // ============================================

    @McpResource(
        uri = "server://info",
        name = "Server Information",
        description = "Get current server status and information",
        mimeType = "text/plain",
        icons = {"https://example.com/info-icon.png:image/png:32x32"}
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
            ✓ Context Access
            ✓ Pre/Post Hooks
            ✓ Async Operations

            CAPABILITIES:
            - Tools with enhanced parameters
            - Resources with multiple formats
            - Prompts with templates
            - Session state management
            - Progress reporting
            - Notification system
            """, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }



    public static void main(String[] args) {
        System.out.println("""
            ╔════════════════════════════════════════════════════════════╗
            ║     FastMCP4J Echo Server v3.0.0 - Full Feature Demo     ║
            ╚════════════════════════════════════════════════════════════╝

            📦 Features Enabled:
               ✓ Memory Tool      (@McpMemory)
               ✓ Todo Tool        (@McpTodo)
               ✓ Planner Tool     (@McpPlanner)
               ✓ FileRead Tool    (@McpFileRead)
               ✓ FileWrite Tool   (@McpFileWrite)
               ✓ Icons Support    (server, tools, resources, prompts)
               ✓ Context Access   (@McpContext)
               ✓ Pre/Post Hooks   (@McpPreHook, @McpPostHook)
               ✓ Async Support    (@McpAsync)

            🛠️  Custom Tools: 8
            📄 Resources: 4
            💬 Prompts: 4

            🌐 Transport: HTTP Streamable
            🔌 Port: 3002
            📍 MCP URI: /mcp

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
