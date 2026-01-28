package com.ultrathink.fastmcp.example;

import com.ultrathink.fastmcp.annotations.McpBash;
import com.ultrathink.fastmcp.annotations.McpServer;
import com.ultrathink.fastmcp.core.FastMCP;

/**
 * Demo server showcasing the @McpBash annotation for shell command execution.
 * <p>
 * This server demonstrates OS-aware bash/shell command execution.
 * When enabled, the tool description dynamically includes:
 * <ul>
 *   <li>Detected OS (Windows, macOS, Linux)</li>
 *   <li>Default shell (cmd.exe, zsh, bash)</li>
 *   <li>Platform information</li>
 * </ul>
 * <p>
 * The execute_command tool automatically uses the appropriate shell for the detected OS.
 * <p>
 * Usage:
 * <pre>
 * java com.ultrathink.fastmcp.example.BashDemoServer
 * </pre>
 * <p>
 * Test with:
 * <pre>
 * echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | java BashDemoServer
 * </pre>
 */
@McpServer(
    name = "bash-demo-server",
    version = "1.0.0",
    description = "Demo server showcasing bash/shell command execution with OS detection"
)
@McpBash(timeout = 60)
public class BashDemoServer {

    public static void main(String[] args) {
        FastMCP.server(BashDemoServer.class)
                .stdio()
                .build();
    }
}
