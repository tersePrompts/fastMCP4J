package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.context.Context;
import com.ultrathink.fastmcp.context.McpContext;
import com.ultrathink.fastmcp.core.FastMCP;
import lombok.Getter;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Full-featured SSE test server with all annotations and async support.
 */
@McpServer(
    name = "sse-full",
    version = "1.0.0",
    instructions = "Full-featured SSE test server"
)
@McpMemory
@McpTodo
@McpPlanner
@McpFileRead
@McpFileWrite
public class SseFullServer {

    // Basic tools
    @McpTool(description = "Echo the message back")
    public String echo(
        @McpParam(description = "Message to echo", required = true) String message,
        @McpContext Context context
    ) {
        return "SSE Echo: " + message;
    }

    @McpTool(description = "Add two numbers")
    public double add(
        @McpParam(description = "First number", required = true) double a,
        @McpParam(description = "Second number", required = true) double b
    ) {
        return a + b;
    }

    // Async tool
    @McpTool(description = "Async task with progress reporting")
    @McpAsync
    public Mono<String> asyncTask(
        @McpParam(description = "Task name") String taskName,
        @McpParam(description = "Duration in seconds", defaultValue = "2", required = false) int durationSeconds,
        @McpContext Context ctx
    ) {
        return Mono.fromRunnable(() -> ctx.info("Starting async task: " + taskName))
            .then(Mono.defer(() -> {
                for (int i = 1; i <= durationSeconds; i++) {
                    ctx.reportProgress(i, durationSeconds, "Step " + i);
                    try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
                ctx.info("Completed async task: " + taskName);
                return Mono.just("Task '" + taskName + "' completed");
            }));
    }

    public static void main(String[] args) {
        System.out.println("Starting SSE Full Server on port 3001...");
        FastMCP.server(SseFullServer.class).sse().port(3001).run();
    }
}
