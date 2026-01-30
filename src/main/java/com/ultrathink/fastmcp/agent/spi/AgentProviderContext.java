package com.ultrathink.fastmcp.agent.spi;

import java.util.Map;

/**
 * Context passed to AgentProvider during initialization.
 * Provides access to server configuration and capabilities.
 */
public final class AgentProviderContext {

    private final String serverName;
    private final String serverVersion;
    private final Map<String, Object> config;

    public AgentProviderContext(String serverName, String serverVersion, Map<String, Object> config) {
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.config = config != null ? Map.copyOf(config) : Map.of();
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(String key, Class<T> type) {
        Object value = config.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public Map<String, Object> getAllConfig() {
        return config;
    }
}
