package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.*;
import com.ultrathink.fastmcp.core.FastMCP;

/**
 * Example server demonstrating manual module registration.
 *
 * <p>In this approach, you explicitly list which classes contain MCP tools.
 * This is fast (no reflection overhead) and gives you full control over
 * which classes are scanned.</p>
 *
 * <p>Benefits of manual modules:</p>
 * <ul>
 *   <li>Fast - no package scanning overhead (~0ms)</li>
 *   <li>Explicit - you control exactly which classes are included</li>
 *   <li>Safe - no accidental inclusion of unwanted classes</li>
 * </ul>
 *
 * <p>Use this when:</p>
 * <ul>
 *   <li>You have a small, well-defined set of tool classes</li>
 *   <li>Performance is critical</li>
 *   <li>You want explicit control over your tool registry</li>
 * </ul>
 */
@McpServer(
    name = "manual-modules-demo",
    version = "1.0.0",
    instructions = "Demo server with manually registered tool modules",
    modules = {
        StringTools.class,
        MathTools.class,
        SystemTools.class
    }
)
public class ManualModulesServer {

    @McpTool(description = "Get server status")
    public String status() {
        return "Server running with 3 tool modules (StringTools, MathTools, SystemTools)";
    }

    public static void main(String[] args) {
        FastMCP.server(ManualModulesServer.class)
            .stdio()
            .run();
    }
}

/**
 * String manipulation tools.
 */
class StringTools {
    @McpTool(description = "Convert string to uppercase")
    public String toUppercase(String text) {
        return text.toUpperCase();
    }

    @McpTool(description = "Convert string to lowercase")
    public String toLowercase(String text) {
        return text.toLowerCase();
    }

    @McpTool(description = "Reverse a string")
    public String reverse(String text) {
        return new StringBuilder(text).reverse().toString();
    }

    @McpTool(description = "Count words in a string")
    public int wordCount(String text) {
        return text.split("\\s+").length;
    }
}

/**
 * Mathematical calculation tools.
 */
class MathTools {
    @McpTool(description = "Add two numbers")
    public double add(double a, double b) {
        return a + b;
    }

    @McpTool(description = "Subtract two numbers")
    public double subtract(double a, double b) {
        return a - b;
    }

    @McpTool(description = "Multiply two numbers")
    public double multiply(double a, double b) {
        return a * b;
    }

    @McpTool(description = "Divide two numbers")
    public double divide(double a, double b) {
        if (b == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return a / b;
    }

    @McpTool(description = "Calculate power")
    public double power(double base, double exponent) {
        return Math.pow(base, exponent);
    }
}

/**
 * System information tools.
 */
class SystemTools {
    @McpResource(uri = "system://info", name = "System Info", mimeType = "text/plain")
    public String getSystemInfo() {
        return String.format("""
            System Information:
            - OS: %s
            - Version: %s
            - Arch: %s
            - Java Version: %s
            """,
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch"),
            System.getProperty("java.version")
        );
    }

    @McpResource(uri = "system://env", name = "Environment Variables", mimeType = "application/json")
    public String getEnvironmentVariables() {
        StringBuilder sb = new StringBuilder("{\n");
        System.getenv().forEach((key, value) ->
            sb.append(String.format("  \"%s\": \"%s\",\n", key, value))
        );
        sb.append("}");
        return sb.toString();
    }

    @McpTool(description = "Get current timestamp in milliseconds")
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @McpTool(description = "Get available processors")
    public int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    @McpTool(description = "Get JVM memory info")
    public String memoryInfo() {
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        long totalMemory = rt.totalMemory();
        long freeMemory = rt.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return String.format(
            "JVM Memory: Used: %d MB, Free: %d MB, Total: %d MB, Max: %d MB",
            usedMemory / (1024 * 1024),
            freeMemory / (1024 * 1024),
            totalMemory / (1024 * 1024),
            maxMemory / (1024 * 1024)
        );
    }
}
