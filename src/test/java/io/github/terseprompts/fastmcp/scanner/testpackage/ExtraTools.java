package io.github.terseprompts.fastmcp.scanner.testpackage;

import io.github.terseprompts.fastmcp.annotations.McpTool;

/**
 * Extra tools for package scanning tests.
 * This class is in a separate package to test auto-discovery via scanBasePackage.
 */
public class ExtraTools {

    @McpTool(description = "Execute a background task")
    public String executeBackgroundTask(String taskId) {
        return "Background task " + taskId + " executed";
    }

    @McpTool(description = "Get system metrics")
    public String getSystemMetrics() {
        return "CPU: 45%, Memory: 2.1GB, Disk: 45%";
    }
}
