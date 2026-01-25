package com.ultrathink.fastmcp.adapter;

import com.ultrathink.fastmcp.context.ContextImpl;
import com.ultrathink.fastmcp.context.McpContext;
import com.ultrathink.fastmcp.model.ToolMeta;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;
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
 * - Build a BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>>
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
    
    // Session state storage - in production this could be backed by Redis, etc.
    private final Map<String, Map<String, Object>> sessionStates = new ConcurrentHashMap<>();

    public ToolHandler(Object instance, ToolMeta meta, ArgumentBinder binder, 
                     ResponseMarshaller marshaller, String serverName) {
        this.instance = instance;
        this.meta = meta;
        this.binder = binder;
        this.marshaller = marshaller;
        this.serverName = serverName;
    }

    public BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> asHandler() {
        return (exchange, request) -> {
            try {
                // Create and set context for this request
                setupContext(exchange);
                
                Object[] args = binder.bind(meta.getMethod(), request.arguments());
                Object result = meta.getMethod().invoke(instance, args);

                Mono<McpSchema.CallToolResult> resultMono;
                if (meta.isAsync()) {
                    resultMono = ((Mono<?>) result).map(marshaller::marshal)
                        .doFinally(signal -> cleanupContext());
                } else {
                    resultMono = Mono.just(marshaller.marshal(result))
                        .doFinally(signal -> cleanupContext());
                }
                
                return resultMono.onErrorResume(e -> {
                    cleanupContext();
                    return Mono.just(errorResult(e));
                });
            } catch (Exception e) {
                cleanupContext();
                return Mono.just(errorResult(e));
            }
        };
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
            "stdio",                    // transport (TODO: determine from exchange)
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
    }
    
    private String getSessionId(McpAsyncServerExchange exchange) {
        // Generate session ID based on thread
        // TODO: Once MCP SDK provides session ID access, use exchange.getSessionId()
        return "session-" + Thread.currentThread().getId();
    }
    
    private String getClientId(McpAsyncServerExchange exchange) {
        // TODO: Once MCP SDK provides client ID access, use exchange.getClientId()
        return "client-unknown";
    }
    
    private Map<String, Object> getMetadata(McpAsyncServerExchange exchange) {
        // Extract metadata from exchange
        return Map.of(); // TODO: Extract actual metadata
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
