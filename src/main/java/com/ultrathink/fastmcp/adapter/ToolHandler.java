package com.ultrathink.fastmcp.adapter;

import com.ultrathink.fastmcp.context.ContextImpl;
import com.ultrathink.fastmcp.context.McpContext;
import com.ultrathink.fastmcp.hook.HookManager;
import com.ultrathink.fastmcp.model.ToolMeta;
import com.ultrathink.fastmcp.telemetry.TelemetryService;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * ToolHandler
 * Bridges a ToolMeta (reflective method) to a Reactor-based MCP tool handler.
 *
 * This class encapsulates how a server object's annotated tool method is invoked
 * when a client calls a tool. It wires together argument binding, method
 * invocation, and response marshalling.
 *
 * Key behavior:
 * - Build a BiFunction&lt;McpAsyncServerExchange, McpSchema.CallToolRequest, Mono&lt;McpSchema.CallToolResult&gt;&gt;
 * - Bind method arguments via ArgumentBinder (with Context injection)
 * - Invoke method reflectively
 * - If tool is marked as async, unwrap Mono/Flux and marshal results
 * - Convert exceptions into error CallToolResults
 */
public class ToolHandler {
    private final Object instance;
    private final ToolMeta meta;
    private final ArgumentBinder binder;
    private final ResponseMarshaller marshaller;
    private final String serverName;
    private final HookManager hookManager;
    private final TelemetryService telemetry;

    // Session state storage - in production this could be backed by Redis, etc.
    private final Map<String, Map<String, Object>> sessionStates = new ConcurrentHashMap<>();

    public ToolHandler(Object instance, ToolMeta meta, ArgumentBinder binder,
                     ResponseMarshaller marshaller, String serverName, HookManager hookManager,
                     TelemetryService telemetry) {
        this.instance = instance;
        this.meta = meta;
        this.binder = binder;
        this.marshaller = marshaller;
        this.serverName = serverName;
        this.hookManager = hookManager;
        this.telemetry = telemetry;
    }

    public ToolHandler(Object instance, ToolMeta meta, ArgumentBinder binder,
                     ResponseMarshaller marshaller, String serverName, HookManager hookManager) {
        this(instance, meta, binder, marshaller, serverName, hookManager, null);
    }

    public BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> asHandler() {
        return (exchange, request) -> {
            Instant start = Instant.now();
            TelemetryService.Span span = telemetry != null
                ? telemetry.createSpan("tool." + meta.getName(), null)
                : null;

            setupContext(exchange);
            try {
                if (hookManager != null) hookManager.executePreHooks(meta.getName(), request.arguments());

                Object[] args = binder.bind(meta.getMethod(), request.arguments());
                Object result = meta.getMethod().invoke(instance, args);

                Mono<McpSchema.CallToolResult> mono;
                if (meta.isAsync()) {
                    mono = ((Mono<?>) result).map(r -> {
                        if (hookManager != null) hookManager.executePostHooks(meta.getName(), request.arguments(), r);
                        return marshaller.marshal(r);
                    });
                } else {
                    if (hookManager != null) hookManager.executePostHooks(meta.getName(), request.arguments(), result);
                    mono = Mono.just(marshaller.marshal(result));
                }

                return mono.doFinally(sig -> {
                    recordTelemetry(start, true);
                    if (span != null) span.close();
                    cleanupContext();
                }).onErrorResume(e -> {
                    recordTelemetry(start, false);
                    if (span != null) span.close();
                    cleanupContext();
                    return Mono.just(errorResult(e));
                });

            } catch (Exception e) {
                recordTelemetry(start, false);
                if (span != null) span.close();
                cleanupContext();
                return Mono.just(errorResult(e));
            }
        };
    }

    private void recordTelemetry(Instant start, boolean success) {
        if (telemetry != null) {
            Duration duration = Duration.between(start, Instant.now());
            telemetry.recordToolInvocation(meta.getName(), duration, success);
        }
    }
    
    /**
     * Set up Context for the current request.
     */
    private void setupContext(McpAsyncServerExchange exchange) {
        String sessionId = getSessionId(exchange);
        Map<String, Object> sessionState = sessionStates.computeIfAbsent(
            sessionId, k -> new ConcurrentHashMap<>()
        );
        
        ContextImpl.RequestContext mcpExchange = new ContextImpl.RequestContext(
            UUID.randomUUID().toString(), // requestId
            getClientId(exchange),       // clientId
            sessionId,                  // sessionId
            getTransportType(exchange),  // transport
            getMetadata(exchange)         // meta
        );
        
        ContextImpl.NotificationHelper notificationHelper = createNotificationHelper(exchange);
        
        ContextImpl context = new ContextImpl(
            mcpExchange,
            serverName,
            sessionState,
            notificationHelper
        );
        
        ContextImpl.setCurrentContext(context);
    }
    
    /**
     * Clean up Context after request completes.
     */
    private void cleanupContext() {
        ContextImpl.clearCurrentContext();
        com.ultrathink.fastmcp.core.FastMCP.clearTransportContext();
    }

    /**
     * Determine the transport type from the MCP exchange.
     * NOTE: The MCP SDK (io.modelcontextprotocol.server.McpAsyncServerExchange)
     * does not currently expose the underlying transport type. This is a limitation
     * of the SDK - once it provides a method like getTransportType(), we should
     * update this implementation to use it. For now, we detect from transport context.
     */
    private String getTransportType(McpAsyncServerExchange exchange) {
        Map<String, String> ctx = com.ultrathink.fastmcp.core.FastMCP.getTransportContext();
        if (ctx != null && ctx.containsKey("_transport")) {
            return ctx.get("_transport");
        }
        return "stdio";  // Default transport
    }

    /**
     * Get a unique session identifier for the current exchange.
     * NOTE: The MCP SDK does not currently expose session IDs directly.
     * We use thread ID as a proxy since stdio connections are typically
     * 1:1 with threads. For HTTP/SSE, proper session tracking would be needed.
     * Future SDK versions may provide exchange.getSessionId().
     */
    private String getSessionId(McpAsyncServerExchange exchange) {
        Map<String, String> ctx = com.ultrathink.fastmcp.core.FastMCP.getTransportContext();
        if (ctx != null && ctx.containsKey("_sessionId")) {
            return ctx.get("_sessionId");
        }
        // Generate session ID based on thread as fallback
        return "session-" + Thread.currentThread().getId();
    }

    /**
     * Get the client identifier for the current exchange.
     * NOTE: The MCP SDK does not currently expose client IDs.
     * For stdio, this is typically the connected process. For HTTP, this would
     * be extracted from headers or authentication. Future SDK versions may
     * provide exchange.getClientId().
     */
    private String getClientId(McpAsyncServerExchange exchange) {
        Map<String, String> ctx = com.ultrathink.fastmcp.core.FastMCP.getTransportContext();
        if (ctx != null && ctx.containsKey("_clientId")) {
            return ctx.get("_clientId");
        }
        return "client-unknown";
    }
    
    private Map<String, Object> getMetadata(McpAsyncServerExchange exchange) {
        // Try to get headers from transport context (for HTTP transports)
        Map<String, String> transportCtx = com.ultrathink.fastmcp.core.FastMCP.getTransportContext();
        if (transportCtx != null && !transportCtx.isEmpty()) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("headers", new HashMap<>(transportCtx));
            // Also add headers directly for convenience
            meta.putAll(transportCtx);
            return meta;
        }
        return Map.of();
    }
    
    /**
     * Create notification helper from exchange.
     * This is a placeholder - actual implementation would use MCP SDK's notification API.
     */
    private ContextImpl.NotificationHelper createNotificationHelper(McpAsyncServerExchange exchange) {
        return new ContextImpl.NotificationHelper() {
            @Override
            public void sendLog(String level, String message) {
                System.err.println("[" + level + "] " + message);
            }
            
            @Override
            public void sendProgress(double progress, double total, String message) {
                System.err.println("[Progress] " + progress + "/" + total + ": " + message);
            }
            
            @Override
            public void sendResourceChange(String action, String uri) {
                System.err.println("[Resource] " + action + ": " + uri);
            }
        };
    }

    private McpSchema.CallToolResult errorResult(Throwable e) {
        return McpSchema.CallToolResult.builder()
            .content(List.of(new McpSchema.TextContent(e.getMessage())))
            .isError(true)
            .build();
    }
}
