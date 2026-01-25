package io.github.fastmcp.adapter;

/**
 * ToolHandler
 * Bridges a ToolMeta (reflective method) to a Reactor-based MCP tool handler.
 *
 * This class encapsulates how a server object's annotated tool method is invoked
 * when a client calls the tool. It wires together argument binding, method
 * invocation, and response marshalling.
 *
 * Key behavior:
 * - Build a BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<CallToolResult>>
 * - Bind method arguments via ArgumentBinder
 * - Invoke the method reflectively
 * - If the tool is marked as async, unwrap the Mono/Flux and marshal results
 * - Convert exceptions into error CallToolResults
 */

import io.github.fastmcp.model.ToolMeta;
import io.github.fastmcp.adapter.ArgumentBinder;
import io.github.fastmcp.adapter.ResponseMarshaller;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.function.BiFunction;
import java.lang.reflect.Method;
import java.util.Map;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.sdk.CallToolRequest;
import io.modelcontextprotocol.sdk.CallToolResult;
import io.modelcontextprotocol.sdk.TextContent;

// Minimal tool handler adapter, wraps annotated method as a Reactor-based handler
public class ToolHandler {
    private final Object instance;
    private final ToolMeta meta;
    private final ArgumentBinder binder;
    private final ResponseMarshaller marshaller;
    private final io.github.fastmcp.hook.HookManager hookManager;

    public ToolHandler(Object instance, ToolMeta meta, ArgumentBinder binder, ResponseMarshaller marshaller, io.github.fastmcp.hook.HookManager hookManager) {
        this.instance = instance;
        this.meta = meta;
        this.binder = binder;
        this.marshaller = marshaller;
        this.hookManager = hookManager;
    }

    public BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<CallToolResult>> asHandler() {
        return (exchange, request) -> {
            try {
                Object[] args = binder.bind(meta.getMethod(), request.arguments());
                
                if (hookManager != null) {
                    hookManager.executePreHooks(meta.getName(), request.arguments());
                }
                
                Object result = meta.getMethod().invoke(instance, args);

                CallToolResult callResult;
                if (meta.isAsync()) {
                    return ((Mono<?>) result).map(r -> {
                        CallToolResult res = marshaller.marshal(r);
                        if (hookManager != null) {
                            hookManager.executePostHooks(meta.getName(), request.arguments(), r);
                        }
                        return res;
                    });
                } else {
                    callResult = marshaller.marshal(result);
                    if (hookManager != null) {
                        hookManager.executePostHooks(meta.getName(), request.arguments(), result);
                    }
                    return Mono.just(callResult);
                }
            } catch (Exception e) {
                return Mono.just(errorResult(e));
            }
        };
    }

    private CallToolResult errorResult(Exception e) {
        return CallToolResult.builder()
            .content(List.of(new TextContent(e.getMessage())))
            .isError(true)
            .build();
    }
}
