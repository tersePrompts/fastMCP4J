package io.github.terseprompts.fastmcp.example;

import io.github.terseprompts.fastmcp.annotations.McpAsync;
import io.github.terseprompts.fastmcp.annotations.McpServer;
import io.github.terseprompts.fastmcp.annotations.McpTool;
import io.github.terseprompts.fastmcp.context.Context;
import io.github.terseprompts.fastmcp.context.ContextImpl;
import io.github.terseprompts.fastmcp.context.McpContext;
import io.github.terseprompts.fastmcp.core.FastMCP;
import reactor.core.publisher.Mono;

/**
 * Example server demonstrating Context Access capabilities.
 * Shows logging, progress reporting, and session state management.
 */
@McpServer(
    name = "Context Example Server",
    version = "1.0.0",
    instructions = "Demonstrates Context API for logging, progress, and session state"
)
public class ContextExampleServer {

    @McpTool(description = "Process data with logging at different levels")
    public String processData(String input, @McpContext Context ctx) {
        ctx.debug("Debug: Starting to process: " + input);
        ctx.info("Info: Beginning processing for input: " + input);
        
        // Simulate processing
        String result = "Processed: " + input.toUpperCase();
        
        ctx.warning("Warning: This is just an example server");
        ctx.info("Info: Processing complete, result: " + result);
        
        return result;
    }

    @McpTool(description = "Report progress for a long-running operation")
    public String longTask(int steps, @McpContext Context ctx) {
        ctx.info("Starting long task with " + steps + " steps");
        
        for (int i = 1; i <= steps; i++) {
            ctx.debug("Executing step " + i + " of " + steps);
            ctx.reportProgress((double) i / steps * 100, 100, "Step " + i + " of " + steps);
        }
        
        ctx.info("Long task completed!");
        return "Completed " + steps + " steps successfully";
    }

    @McpTool(description = "Increment a counter stored in session state")
    public int incrementCounter(@McpContext Context ctx) {
        Integer count = (Integer) ctx.getState("counter");
        int newCount = (count == null) ? 1 : count + 1;
        ctx.setState("counter", newCount);
        
        ctx.info("Counter incremented to: " + newCount);
        return newCount;
    }

    @McpTool(description = "Get current counter value from session state (null if not set)")
    public Integer getCounter(@McpContext Context ctx) {
        Integer count = (Integer) ctx.getState("counter");
        ctx.info("Retrieved counter: " + (count != null ? count : "not set"));
        return count;
    }

    @McpTool(description = "Reset counter in session state")
    public String resetCounter(@McpContext Context ctx) {
        ctx.deleteState("counter");
        ctx.info("Counter reset");
        return "Counter has been reset";
    }

    @McpTool(description = "Get request and server information")
    public String getRequestInfo(@McpContext Context ctx) {
        StringBuilder info = new StringBuilder();
        
        info.append("Request Info:\n");
        info.append("  Request ID: ").append(ctx.getRequestId()).append("\n");
        info.append("  Client ID: ").append(ctx.getClientId()).append("\n");
        info.append("  Session ID: ").append(ctx.getSessionId()).append("\n");
        info.append("  Transport: ").append(ctx.getTransport()).append("\n");
        info.append("  Server Name: ").append(ctx.getServerName()).append("\n");
        
        ctx.info("Request info retrieved");
        return info.toString();
    }

    @McpTool(description = "Store arbitrary data in session state")
    public String storeData(String key, String value, @McpContext Context ctx) {
        ctx.setState(key, value);
        ctx.info("Stored data: " + key + " = " + value);
        return "Stored " + key + " = " + value;
    }

    @McpTool(description = "Retrieve data from session state")
    public String getData(String key, @McpContext Context ctx) {
        Object value = ctx.getState(key);
        String result = (value != null) ? value.toString() : "null (not found)";
        ctx.info("Retrieved " + key + " = " + result);
        return result;
    }

    @McpTool(description = "Demonstrate deep context access without parameter")
    public String analyzeDataset(String datasetName) {
        // Access context without it being passed as parameter
        Context ctx = ContextImpl.getCurrentContext();
        ctx.info("Analyzing dataset: " + datasetName);
        ctx.debug("Starting analysis...");
        
        ctx.reportProgress(50, 100, "Analyzing dataset structure");
        
        ctx.info("Analysis complete for: " + datasetName);
        return "Analysis complete for dataset: " + datasetName;
    }

    @McpTool(description = "Async task with progress reporting")
    @McpAsync
    public Mono<String> asyncTask(int duration, @McpContext Context ctx) {
        return Mono.fromRunnable(() -> {
            ctx.info("Starting async task of " + duration + " seconds");
        })
        .then(Mono.defer(() -> {
            ctx.reportProgress(25, 100, "Initialization complete");
            return Mono.just(1);
        }))
        .delayElement(java.time.Duration.ofSeconds(duration / 4))
        .flatMap(step -> {
            ctx.reportProgress(50, 100, "Processing step " + step);
            return Mono.just(step + 1);
        })
        .delayElement(java.time.Duration.ofSeconds(duration / 4))
        .flatMap(step -> {
            ctx.reportProgress(75, 100, "Processing step " + step);
            return Mono.just(step + 1);
        })
        .delayElement(java.time.Duration.ofSeconds(duration / 4))
        .doOnSuccess(step -> {
            ctx.reportProgress(100, 100, "Async task complete");
            ctx.info("Async task completed successfully");
        })
        .map(step -> "Async task completed in " + step + " steps");
    }

    public static void main(String[] args) {
        FastMCP.server(ContextExampleServer.class)
            .stdio()
            .run();
    }
}
