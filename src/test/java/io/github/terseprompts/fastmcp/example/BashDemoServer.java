package io.github.terseprompts.fastmcp.example;

import io.github.terseprompts.fastmcp.annotations.BashMode;
import io.github.terseprompts.fastmcp.annotations.McpBash;
import io.github.terseprompts.fastmcp.annotations.McpServer;
import io.github.terseprompts.fastmcp.core.FastMCP;

/**
 * Demo server showcasing the {@link McpBash} annotation.
 *
 * <p>This server uses the default <b>sandbox mode</b>: scripts run in a
 * bashkit4j in-memory virtual computer. The host machine is unreachable —
 * only mounted, allowlisted directories are visible (none mounted here).
 * State persists across tool calls (cwd, env, files).
 *
 * <p>For trusted local automation, host mode runs the real shell:
 * <pre>{@code
 * @McpBash(mode = BashMode.HOST, timeout = 60,
 *          notAllowedPaths = {"/etc", "/root"})
 * }</pre>
 */
@McpServer(
    name = "bash-demo-server",
    version = "1.0.0"
)
@McpBash(mode = BashMode.SANDBOX, timeout = 60)
public class BashDemoServer {

    public static void main(String[] args) {
        FastMCP.server(BashDemoServer.class)
                .stdio()
                .build();
    }
}
