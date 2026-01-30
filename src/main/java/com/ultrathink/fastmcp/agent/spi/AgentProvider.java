package com.ultrathink.fastmcp.agent.spi;

import java.util.List;
import com.ultrathink.fastmcp.model.ToolMeta;
import com.ultrathink.fastmcp.model.ResourceMeta;
import com.ultrathink.fastmcp.model.PromptMeta;

/**
 * Service Provider Interface for agent bundles.
 * Implementations are discovered via Java ServiceLoader.
 *
 * Register in META-INF/services/com.ultrathink.fastmcp.agent.spi.AgentProvider
 */
public interface AgentProvider {

    /**
     * Provider name (e.g., "todo", "planner", "memory")
     */
    String getName();

    /**
     * Provider version
     */
    String getVersion();

    /**
     * Initialize the provider with FastMCP configuration
     */
    void initialize(AgentProviderContext context);

    /**
     * Get tools provided by this agent (optional, empty if none)
     */
    default List<ToolMeta> getTools() {
        return List.of();
    }

    /**
     * Get resources provided by this agent (optional, empty if none)
     */
    default List<ResourceMeta> getResources() {
        return List.of();
    }

    /**
     * Get prompts provided by this agent (optional, empty if none)
     */
    default List<PromptMeta> getPrompts() {
        return List.of();
    }

    /**
     * Called when server starts
     */
    default void onServerStart() {}

    /**
     * Called when server stops
     */
    default void onServerStop() {}

    /**
     * Called when a session starts
     */
    default void onSessionStart(String sessionId) {}

    /**
     * Called when a session ends
     */
    default void onSessionEnd(String sessionId) {}

    /**
     * Cleanup resources
     */
    default void cleanup() {}
}
