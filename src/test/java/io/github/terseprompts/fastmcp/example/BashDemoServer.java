package io.github.terseprompts.fastmcp.example;

import io.github.terseprompts.fastmcp.annotations.McpBash;
import io.github.terseprompts.fastmcp.annotations.McpServer;
import io.github.terseprompts.fastmcp.core.FastMCP;

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
 * java io.github.terseprompts.fastmcp.example.BashDemoServer
 * </pre>
 * <p>
 * Test with:
 * <pre>
 * echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | java BashDemoServer
 * </pre>
 */
@McpServer(
    name = "bash-demo-server",
    version = "1.0.0"
)
@McpBash(timeout = 60)
public class BashDemoServer {

    public static void main(String[] args) {
        FastMCP.server(BashDemoServer.class)
                .stdio()
                .build();
    }
}
