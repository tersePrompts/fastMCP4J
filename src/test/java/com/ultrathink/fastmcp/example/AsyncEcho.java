package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.McpAsync;
import com.ultrathink.fastmcp.annotations.McpParam;
import com.ultrathink.fastmcp.annotations.McpServer;
import com.ultrathink.fastmcp.annotations.McpTool;
import com.ultrathink.fastmcp.context.Context;
import com.ultrathink.fastmcp.context.McpContext;
import com.ultrathink.fastmcp.core.FastMCP;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AsyncEcho - Demonstrates async operations and progress reporting.
 *
 * FEATURES:
 * - @McpAsync: Reactive operations with Mono/Flux
 * - Context access for progress reporting and notifications
 * - Long-running task simulation
 */
@McpServer(
    name = "async-echo",
    version = "1.0.0",
    instructions = "Async operations demo server with progress reporting",
    icons = {
        "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjY0IiBoZWlnaHQ9IjY0IiByeD0iMTIiIGZpbGw9IiM4NDQ1OTkiLz4KPHBhdGggZD0iTTMyIDE2QzIzLjE2MzQgMTYgMTYgMjMuMTYzNCAxNiAzMkMxNiA0MC44MzY2IDIzLjE2MzQgNDggMzIgNDhDMzguNjQ1IDQ4IDQ0LjM0MjIgNDQuMTM1NiA0Ny4xNDI4IDM4LjgzNDRMNTQuMjg1NiA0Ni45NzkyQzUwLjQxMTQgNTQuODUzMiA0MS44MzYgNjAgMzIgNjBDMjAuOTU0MyA2MCAxMiA1MS4wNDU3IDEyIDQwQzEyIDMzLjM2NDkgMTUuODY0NCAyNy42NTc4IDIxLjE2NTYgMjQuODU3MkwxMy4wMjA4IDE3LjcxNDlDMTYuODk1IDkuODM2MDkgMjUuNDcwNSA0IDMyIDRDNDMuMDQ1NyA0IDUyIDEyLjk1NDMgNTIgMjRDNTIgMzAuNjM1MSA0OC4xMzU2IDM2LjM0MjIgNDIuODM0NCAzOS4xNDI4TDUwLjk3OTIgNDYuOTc5MkM0Ny4xMDUgNTQuODU3MSAzOC41Mjk1IDYwIDMyIDYwQzIwLjk1NDMgNjAgMTIgNTEuMDQ1NyAxMiA0MEMxMiAzMy4zNjQ5IDE1Ljg2NDQgMjcuNjU3OCAyMS4xNjU2IDI0Ljg1NzJMMzIgMTZaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTYgOEMxMS41ODE3IDggOCAxMS41ODE3IDggMTZDOCAyMC40MTgzIDExLjU4MTcgMjQgMTYgMjRDMjAuNDE4MyAyNCAyNCAyMC40MTgzIDI0IDE2QzI0IDExLjU4MTcgMjAuNDE4MyA4IDE2IDhaIiBmaWxsPSJ3aGl0ZSIvPgo8L3N2Zz4=:image/svg+xml:64x64:light",
        "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjY0IiBoZWlnaHQ9IjY0IiByeD0iMTIiIGZpbGw9IiM1QjIxQjYiLz4KPHBhdGggZD0iTTMyIDE2QzIzLjE2MzQgMTYgMTYgMjMuMTYzNCAxNiAzMkMxNiA0MC44MzY2IDIzLjE2MzQgNDggMzIgNDhDMzguNjQ1IDQ4IDQ0LjM0MjIgNDQuMTM1NiA0Ny4xNDI4IDM4LjgzNDRMNTQuMjg1NiA0Ni45NzkyQzUwLjQxMTQgNTQuODUzMiA0MS44MzYgNjAgMzIgNjBDMjAuOTU0MyA2MCAxMiA1MS4wNDU3IDEyIDQwQzEyIDMzLjM2NDkgMTUuODY0NCAyNy42NTc4IDIxLjE2NTYgMjQuODU3MkwxMy4wMjA4IDE3LjcxNDlDMTYuODk1IDkuODM2MDkgMjUuNDcwNSA0IDMyIDRDNDMuMDQ1NyA0IDUyIDEyLjk1NDMgNTIgMjRDNTIgMzAuNjM1MSA0OC4xMzU2IDM2LjM0MjIgNDIuODM0NCAzOS4xNDI4TDUwLjk3OTIgNDYuOTc5MkM0Ny4xMDUgNTQuODU3MSAzOC41Mjk1IDYwIDMyIDYwQzIwLjk1NDMgNjAgMTIgNTEuMDQ1NyAxMiA0MEMxMiAzMy4zNjQ5IDE1Ljg2NDQgMjcuNjU3OCAyMS4xNjU2IDI0Ljg1NzJMMzIgMTZaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTYgOEMxMS41ODE3IDggOCAxMS41ODE3IDggMTZDOCAyMC40MTgzIDExLjU4MTcgMjQgMTYgMjRDMjAuNDE4MyAyNCAyNCAyMC40MTgzIDI0IDE2QzI0IDExLjU4MTcgMjAuNDE4MyA4IDE2IDhaIiBmaWxsPSJ3aGl0ZSIvPgo8L3N2Zz4=:image/svg+xml:64x64:dark"
    }
)
public class AsyncEcho {

    /**
     * Process task with progress reporting.
     * Demonstrates synchronous progress updates via Context.
     */
    @McpTool(
        description = "Process task with progress reporting using Context",
        icons = {"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjMyIiBoZWlnaHQ9IjMyIiByeD0iOCIgZmlsbD0iIzEwQjk4MSIvPgo8Y2lyY2xlIGN4PSIxNiIgY3k9IjE2IiByPSI4IiBzdHJva2U9IndoaXRlIiBzdHJva2Utd2lkdGg9IjIiIGZpbGw9Im5vbmUiLz4KPHBhdGggZD0iTTE2IDE2VjEyIiBzdHJva2U9IndoaXRlIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPgo8L3N2Zz4=:image/svg+xml:32x32"}
    )
    public String processWithProgress(
        @McpParam(
            description = "Number of steps to process",
            examples = {"10", "50", "100"},
            constraints = "Must be positive"
        )
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

    /**
     * Async task with progress reporting.
     * Demonstrates reactive programming with Mono and @McpAsync.
     */
    @McpTool(
        description = "Async task with progress reporting",
        icons = {"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjMyIiBoZWlnaHQ9IjMyIiByeD0iOCIgZmlsbD0iIzg0NDU5OSIvPgo8cGF0aCBkPSJNMTYgOEMxMS41ODE3IDggOCAxMS41ODE3IDggMTZDOCAyMC40MTgzIDExLjU4MTcgMjQgMTYgMjRDMjAuNDE4MyAyNCAyNCAyMC40MTgzIDI0IDE2QzI0IDExLjU4MTcgMjAuNDE4MyA4IDE2IDhaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTYgMTJWMjAiIHN0cm9rZT0iIzg0NDU5OSIgc3Ryb2tlLXdpZHRoPSIzIiBzdHJva2UtbGluZWNhcD0icm91bmQiLz4KPHAJdGggZD0iTTE2IDEyTDIwIDE2TDE2IDIwTDEyIDE2TDE2IDEyWiIgZmlsbD0iIzg0NDU5OSIvPgo8L3N2Zz4=:image/svg+xml:32x32"}
    )
    @McpAsync
    public Mono<String> asyncTask(
        @McpParam(
            description = "Task name to execute",
            examples = {"backup", "sync", "analyze"},
            constraints = "Cannot be empty"
        )
        String taskName,

        @McpParam(
            description = "Duration in seconds",
            examples = {"3", "5", "10"},
            constraints = "Must be positive",
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

    public static void main(String[] args) {
        System.out.println("""
            ╔════════════════════════════════════════════════════════════╗
            ║     FastMCP4J AsyncEcho v1.0.0 - Async Operations Demo     ║
            ╚════════════════════════════════════════════════════════════╝

            🚀 Features:
               ✓ @McpAsync operations (Mono/Flux)
               ✓ Progress reporting via Context
               ✓ Notification system

            🛠️  Tools: 2
               - processWithProgress: Synchronous with progress
               - asyncTask: Reactive async with progress

            🌐 Transport: HTTP Streamable
            🔌 Port: 3003
            📍 MCP URI: /mcp

            """);

        FastMCP.server(AsyncEcho.class)
            .streamable()
            .port(3003)
            .mcpUri("/mcp")
            .run();
    }
}
