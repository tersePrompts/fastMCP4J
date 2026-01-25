package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.core.FastMCP;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * Example demonstrating pre and post hooks for MCP tools.
 *
 * This example shows:
 * - Tool-specific hooks (@McpPreHook with toolName)
 * - Method name-inferred hooks (@McpPreHook without toolName)
 * - Global hooks (toolName = "*")
 * - Hooks with async tools
 */
@McpServer(name = "HooksExample", version = "1.0.0")
public class HooksExample {

    @McpPreHook(toolName = "calculate")
    public void preCalculateHook(Map<String, Object> args) {
        System.out.println("[PRE-HOOK] About to execute calculate with args: " + args);
    }

    @McpPostHook(toolName = "calculate")
    public void postCalculateHook(Map<String, Object> args, Object result) {
        System.out.println("[POST-HOOK] calculate completed with result: " + result);
    }

    @McpTool(description = "Calculate sum of two numbers")
    public int calculate(int a, int b) {
        int result = a + b;
        System.out.println("[TOOL] Adding " + a + " + " + b + " = " + result);
        return result;
    }

    @McpPreHook
    public void preSearch(Map<String, Object> args) {
        System.out.println("[PRE-HOOK] Searching with query: " + args.get("query"));
    }

    @McpPostHook
    public void postSearch(Map<String, Object> args, Object result) {
        System.out.println("[POST-HOOK] Search returned: " + result);
    }

    @McpTool(description = "Search for items")
    public String search(String query) {
        System.out.println("[TOOL] Executing search for: " + query);
        return "Results for: " + query;
    }

    @McpPreHook(toolName = "*")
    public void globalPreHook(Map<String, Object> args) {
        System.out.println("[GLOBAL PRE-HOOK] Tool called at: " + System.currentTimeMillis());
    }

    @McpPostHook(toolName = "*")
    public void globalPostHook(Map<String, Object> args, Object result) {
        System.out.println("[GLOBAL POST-HOOK] Tool finished at: " + System.currentTimeMillis());
    }

    @McpTool(description = "Long running async task")
    @McpAsync
    public Mono<String> asyncTask(String input) {
        System.out.println("[TOOL] Starting async task");
        return Mono.fromCallable(() -> {
            Thread.sleep(100);
            return "Processed: " + input;
        });
    }

    public static void main(String[] args) {
        FastMCP.server(HooksExample.class)
            .stdio()
            .run();
    }
}
