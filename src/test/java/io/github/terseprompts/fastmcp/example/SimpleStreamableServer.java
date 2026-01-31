package io.github.terseprompts.fastmcp.example;

import io.github.terseprompts.fastmcp.annotations.*;
import io.github.terseprompts.fastmcp.core.FastMCP;

/**
 * Simple Streamable test server - minimal configuration for CI/CD.
 */
@McpServer(name = "simple-streamable", version = "1.0.0")
public class SimpleStreamableServer {

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
        System.out.println("Starting Simple Streamable Server on port 3002...");
        FastMCP.server(SimpleStreamableServer.class).streamable().port(3002).run();
    }
}
