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
        "https://example.com/server-icon.png:image/png:64x64:light",
        "https://example.com/server-icon-dark.png:image/png:64x64:dark"
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
        icons = {"https://example.com/echo-icon.png:image/png:32x32"}
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
        icons = {"https://example.com/calc-icon.png:image/png:32x32"}
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
        icons = {"https://example.com/progress-icon.png:image/png:32x32"}
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
        icons = {"https://example.com/async-icon.png:image/png:32x32"}
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
