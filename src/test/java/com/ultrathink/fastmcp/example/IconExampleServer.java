package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;

/**
 * Example server demonstrating the Icons feature.
 * Icons can be attached to servers, tools, resources, and prompts.
 */
@McpServer(
    name = "Icon Example Server",
    version = "1.0.0",
    instructions = "A server demonstrating MCP icon support.",
    icons = {
        // Simple icon with just src
        "https://example.com/server-icon.png",
        // Full icon specification
        "https://example.com/server-icon-dark.png:image/png:48x48:dark",
        // Scalable SVG icon
        "https://example.com/server-icon.svg:image/svg+xml:any:light"
    }
)
public class IconExampleServer {

    @McpTool(
        name = "echo",
        description = "Echoes back the input text",
        icons = {
            "https://example.com/echo-icon.png:image/png:48x48"
        }
    )
    public String echo(String input) {
        return input;
    }

    @McpTool(
        name = "calculate",
        description = "Performs basic arithmetic calculations",
        icons = {
            "https://example.com/calc-icon-light.png:image/png:48x48:light",
            "https://example.com/calc-icon-dark.png:image/png:48x48:dark"
        }
    )
    public double calculate(double a, double b, String operation) {
        switch (operation.toLowerCase()) {
            case "add":
                return a + b;
            case "subtract":
                return a - b;
            case "multiply":
                return a * b;
            case "divide":
                if (b == 0) {
                    throw new IllegalArgumentException("Cannot divide by zero");
                }
                return a / b;
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }

    @McpResource(
        uri = "file:///config.json",
        name = "Configuration",
        description = "Server configuration file",
        mimeType = "application/json",
        icons = {
            "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSJjdXJyZW50Q29sb3IiIHN0cm9rZS13aWR0aD0iMiI+PHBhdGggZD0iTTE0IDJIMmEyIDIgMCAwIDAtMiAydjE0YTIgMiAwIDAgMCAyIDJoMjBhMiAyIDAgMCAwIDItMlY4bC02LTZ6Ii8+PHBhdGggZD0iTTE0IDJ2Nm02IDBoLTYiLz48L3N2Zz4=:any"
        }
    )
    public String getConfig() {
        return "{\"enabled\": true, \"version\": \"1.0.0\"}";
    }

    @McpResource(
        uri = "file:///logs.txt",
        name = "Logs",
        description = "Server log file",
        mimeType = "text/plain",
        icons = {
            "https://example.com/log-icon.png:image/png:48x48"
        }
    )
    public String getLogs() {
        return "[INFO] Server started\n[INFO] Processing requests...";
    }

    @McpPrompt(
        name = "help",
        description = "Get help with using this server",
        icons = {
            "https://example.com/help-icon.png:image/png:48x48"
        }
    )
    public String help() {
        return "This is an example server demonstrating MCP icon support.\n" +
               "Available tools: echo, calculate\n" +
               "Available resources: config, logs";
    }

    @McpPrompt(
        name = "greeting",
        description = "Generate a greeting message",
        icons = {
            "https://example.com/greeting-light.png:image/png:48x48:light",
            "https://example.com/greeting-dark.png:image/png:48x48:dark"
        }
    )
    public String greeting(String name) {
        return "Hello, " + name + "! Welcome to the Icon Example Server.";
    }
}
