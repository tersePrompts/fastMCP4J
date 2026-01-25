package com.ultrathink.fastmcp.context;

import java.util.Map;
import java.util.Optional;

/**
 * Context information for MCP request execution.
 * Provides access to client metadata, request details, and session information.
 *
 * @version 0.2.0
 * @status NOT_IMPLEMENTED
 */
public interface McpContext {

    /**
     * Get the client identifier
     * @return client ID if available
     */
    Optional<String> getClientId();

    /**
     * Get client name
     * @return client name if available
     */
    Optional<String> getClientName();

    /**
     * Get client version
     * @return client version if available
     */
    Optional<String> getClientVersion();

    /**
     * Get request headers
     * @return map of headers
     */
    Map<String, String> getHeaders();

    /**
     * Get session identifier
     * @return session ID if available
     */
    Optional<String> getSessionId();

    /**
     * Get transport type (stdio, sse, http_streamable)
     * @return transport type
     */
    String getTransportType();

    /**
     * Get custom context attribute
     * @param key attribute key
     * @return attribute value if present
     */
    Optional<Object> getAttribute(String key);

    /**
     * Set custom context attribute
     * @param key attribute key
     * @param value attribute value
     */
    void setAttribute(String key, Object value);

    /**
     * Get current context from ThreadLocal
     * @return current context
     * @throws IllegalStateException if no context is bound to current thread
     */
    static McpContext current() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
