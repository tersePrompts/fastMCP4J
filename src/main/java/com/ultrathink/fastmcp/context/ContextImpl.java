package com.ultrathink.fastmcp.context;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete implementation of Context interface.
 * Provides access to MCP capabilities during request execution.
 * Each request gets a new Context instance.
 */
@Slf4j
public class ContextImpl implements Context {
    
    private final RequestContext exchange;
    private final String serverName;
    private final Map<String, Object> sessionState;
    private final NotificationHelper notificationHelper;
    
    public ContextImpl(RequestContext exchange, String serverName,
                     Map<String, Object> sessionState, NotificationHelper notificationHelper) {
        this.exchange = exchange;
        this.serverName = serverName;
        this.sessionState = sessionState;
        this.notificationHelper = notificationHelper;
    }
    
    @Override
    public void debug(String message) {
        log.debug("{}", message);
        notificationHelper.sendLog("debug", message);
    }
    
    @Override
    public void info(String message) {
        log.info("{}", message);
        notificationHelper.sendLog("info", message);
    }
    
    @Override
    public void warning(String message) {
        log.warn("{}", message);
        notificationHelper.sendLog("warning", message);
    }
    
    @Override
    public void error(String message) {
        log.error("{}", message);
        notificationHelper.sendLog("error", message);
    }
    
    @Override
    public void reportProgress(double progress, double total) {
        reportProgress(progress, total, null);
    }
    
    @Override
    public void reportProgress(double progress, double total, String message) {
        notificationHelper.sendProgress(progress, total, message);
    }
    
    @Override
    public List<ResourceInfo> listResources() {
        log.debug("Listing resources");
        // TODO: Integrate with actual server resource registry
        return List.of();
    }
    
    @Override
    public String readResource(String uri) {
        log.debug("Reading resource: {}", uri);
        notificationHelper.sendResourceChange("read", uri);
        // TODO: Implement actual resource reading
        return "Resource content for: " + uri;
    }
    
    @Override
    public List<PromptInfo> listPrompts() {
        log.debug("Listing prompts");
        // TODO: Integrate with actual server prompt registry
        return List.of();
    }
    
    @Override
    public PromptResult getPrompt(String name, Map<String, Object> arguments) {
        log.debug("Getting prompt: {} with arguments: {}", name, arguments);
        // TODO: Implement actual prompt retrieval
        List<PromptMessage> messages = List.of(
            new PromptMessage("user", "This is a placeholder prompt for: " + name)
        );
        return new PromptResult(messages);
    }
    
    @Override
    public void setState(String key, Object value) {
        sessionState.put(key, value);
        log.debug("Set session state: {} = {}", key, value);
    }
    
    @Override
    public Object getState(String key) {
        Object value = sessionState.get(key);
        log.debug("Get session state: {} = {}", key, value);
        return value;
    }
    
    @Override
    public void deleteState(String key) {
        sessionState.remove(key);
        log.debug("Delete session state: {}", key);
    }
    
    @Override
    public String getRequestId() {
        return exchange.getRequestId();
    }
    
    @Override
    public String getClientId() {
        return exchange.getClientId();
    }
    
    @Override
    public String getSessionId() {
        String sessionId = exchange.getSessionId();
        if (sessionId == null) {
            throw new IllegalStateException("MCP session not established yet");
        }
        return sessionId;
    }
    
    @Override
    public String getTransport() {
        return exchange.getTransport();
    }
    
    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getHeaders() {
        // Headers are stored in meta under "headers" key
        Object headers = exchange.getMeta().get("headers");
        if (headers instanceof Map) {
            return new HashMap<>((Map<String, String>) headers);
        }
        return Map.of();
    }
    
    /**
     * Simple request context representation.
     * This encapsulates request metadata and client information.
     */
    public static class RequestContext {
        private final String requestId;
        private final String clientId;
        private final String sessionId;
        private final String transport;
        private final Map<String, Object> meta;
        
        public RequestContext(String requestId, String clientId, String sessionId,
                         String transport, Map<String, Object> meta) {
            this.requestId = requestId;
            this.clientId = clientId;
            this.sessionId = sessionId;
            this.transport = transport;
            this.meta = meta != null ? meta : new HashMap<>();
        }
        
        public String getRequestId() { return requestId; }
        public String getClientId() { return clientId; }
        public String getSessionId() { return sessionId; }
        public String getTransport() { return transport; }
        public Map<String, Object> getMeta() { return meta; }

        /** Get HTTP headers from the request (for HTTP transports) */
        @SuppressWarnings("unchecked")
        public Map<String, String> getHeaders() {
            // Headers are stored in meta as Map<String, String>
            Object headers = meta.get("headers");
            if (headers instanceof Map) {
                return (Map<String, String>) headers;
            }
            // Search for any header-like entries in meta
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : meta.entrySet()) {
                if (entry.getValue() instanceof String) {
                    result.put(entry.getKey(), (String) entry.getValue());
                }
            }
            return result;
        }
    }
    
    /**
     * ThreadLocal storage for current context.
     * Allows accessing context from deeply nested code without parameter passing.
     */
    private static final ThreadLocal<Context> CURRENT_CONTEXT = new ThreadLocal<>();
    
    /**
     * Set the current context for this thread.
     * Called by the framework during request processing.
     */
    public static void setCurrentContext(Context context) {
        CURRENT_CONTEXT.set(context);
    }
    
    /**
     * Get the current context for this thread.
     * @return Current context, or null if no request is active
     * @throws IllegalStateException if called outside of a request
     */
    public static Context getCurrentContext() {
        Context ctx = CURRENT_CONTEXT.get();
        if (ctx == null) {
            throw new IllegalStateException("No active MCP request context. " +
                "getCurrentContext() can only be called within a request.");
        }
        return ctx;
    }
    
    /**
     * Clear the current context for this thread.
     * Called after request processing completes.
     */
    public static void clearCurrentContext() {
        CURRENT_CONTEXT.remove();
    }
    
    /**
     * Helper interface for sending notifications.
     * Abstracts away the actual notification mechanism.
     */
    public interface NotificationHelper {
        void sendLog(String level, String message);
        void sendProgress(double progress, double total, String message);
        void sendResourceChange(String action, String uri);
    }
}
