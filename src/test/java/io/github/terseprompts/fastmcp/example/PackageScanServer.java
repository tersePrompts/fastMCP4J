package io.github.terseprompts.fastmcp.example;

import io.github.terseprompts.fastmcp.annotations.*;
import io.github.terseprompts.fastmcp.core.FastMCP;

/**
 * Example server demonstrating package scanning for tool discovery.
 *
 * <p>In this approach, the server automatically scans a base package and
 * discovers all classes with @McpTool, @McpResource, and @McpPrompt annotations.</p>
 *
 * <p>Benefits of package scanning:</p>
 * <ul>
 *   <li>Convenient - no need to manually list each class</li>
 *   <li>Auto-discovers new tools as you add them</li>
 *   <li>Scales well for large projects with many tool classes</li>
 * </ul>
 *
 * <p>Trade-offs:</p>
 * <ul>
 *   <li>Slower startup (~10-50ms one-time cost for package scanning)</li>
 *   <li>May discover classes you didn't intend to include</li>
 * </ul>
 *
 * <p>Use this when:</p>
 * <ul>
 *   <li>You have many tool classes</li>
 *   <li>You frequently add new tools</li>
 *   <li>Startup time is not critical</li>
 * </ul>
 */
@McpServer(
    name = "package-scan-demo",
    version = "1.0.0",
    instructions = "Demo server with package-scanned tool discovery",
    scanBasePackage = "io.github.terseprompts.fastmcp.example.tools"
)
public class PackageScanServer {

    @McpTool(description = "Get server status")
    public String status() {
        return "Server running with auto-discovered tools from io.github.terseprompts.fastmcp.example.tools package";
    }

    public static void main(String[] args) {
        FastMCP.server(PackageScanServer.class)
            .stdio()
            .run();
    }
}
