package io.github.terseprompts.fastmcp.mcptools.bash;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Common surface of the two bash tool implementations so {@code FastMCP} can
 * register either uniformly. Implemented by {@link BashTool} (real host
 * shell) and {@link SandboxBashTool} (bashkit4j in-memory sandbox).
 */
public interface BashExecutor extends AutoCloseable {

    /** Execute a script/command with the configured timeout. */
    BashResult executeCommand(String script);

    /** Execute a script/command with an explicit timeout in seconds. */
    BashResult executeCommand(String script, int timeoutSeconds);

    /** Human/LLM-facing tool description (embeds mode/platform info). */
    String getToolDescription();

    /** JSON Schema (2020-12) for the tool's input. */
    String getToolSchema();

    /** MCP tool entry point invoked reflectively by {@code buildBuiltinTool}. */
    Mono<McpSchema.CallToolResult> handleToolCall(McpAsyncServerExchange exchange, Map<String, Object> arguments);

    /** Release resources (native sandbox / child processes). */
    @Override
    void close();
}
