package io.github.terseprompts.fastmcp.example;

import io.github.terseprompts.fastmcp.annotations.*;
import io.github.terseprompts.fastmcp.context.Context;
import io.github.terseprompts.fastmcp.context.McpContext;
import io.github.terseprompts.fastmcp.core.FastMCP;

/**
 * Full-featured STDIO test server with all annotations.
 */
@McpServer(
    name = "stdio-full",
    version = "1.0.0",
    instructions = "Full-featured STDIO test server"
)
@McpMemory
@McpTodo
@McpPlanner
@McpFileRead
@McpFileWrite
public class StdioFullServer {

    // Basic tools
    @McpTool(description = "Echo the message back")
    public String echo(
        @McpParam(description = "Message to echo", required = true) String message,
        @McpContext Context context
    ) {
        return "STDIO Echo: " + message;
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
        FastMCP.server(StdioFullServer.class).stdio().run();
    }
}
