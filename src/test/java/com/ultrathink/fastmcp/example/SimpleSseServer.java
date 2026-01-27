package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.core.FastMCP;

/**
 * Simple SSE test server - minimal configuration for CI/CD.
 */
@McpServer(name = "simple-sse", version = "1.0.0")
public class SimpleSseServer {

    @McpTool(description = "Add two numbers")
    public double add(
        @McpParam(description = "First number", required = true) double a,
        @McpParam(description = "Second number", required = true) double b
    ) {
        return a + b;
    }

    @McpTool(description = "Echo message")
    public String echo(@McpParam(description = "Message") String message) {
        return "Echo: " + message;
    }

    public static void main(String[] args) {
        System.out.println("Starting Simple SSE Server on port 3001...");
        FastMCP.server(SimpleSseServer.class).sse().port(3001).run();
    }
}
