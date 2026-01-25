package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.McpServer;
import com.ultrathink.fastmcp.annotations.McpTool;
import com.ultrathink.fastmcp.core.FastMCP;

@McpServer(name = "echo", version = "1.0.0", instructions = "Simple echo server")
public class EchoServer {

    @McpTool(description = "Echo back the message")
    public String echo(String message) {
        return "Echo: " + message;
    }

    @McpTool(description = "Add two numbers")
    public int add(int a, int b) {
        return a + b;
    }

    public static void main(String[] args) {
        FastMCP.server(EchoServer.class)
            .streamable()
               // .sse()
                .port(3002)
            .mcpUri("/mcp")  // Optional, defaults to "/mcp"
            .run();
    }
}
