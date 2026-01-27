package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.context.Context;
import com.ultrathink.fastmcp.context.McpContext;
import com.ultrathink.fastmcp.core.FastMCP;

/**
 * Full-featured SSE test server with all annotations.
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

    @McpTool(description = "Simple async task")
    @McpAsync
    public reactor.core.publisher.Mono<String> asyncTask(
        @McpParam(description = "Task name") String taskName,
        @McpContext Context ctx
    ) {
        return reactor.core.publisher.Mono.fromSupplier(() -> {
            ctx.info("Completed async task: " + taskName);
            return "Task '" + taskName + "' completed";
        });
    }

    public static void main(String[] args) {
        System.out.println("Starting SSE Full Server on port 3001...");
        FastMCP.server(SseFullServer.class).sse().port(3001).run();
    }
}
