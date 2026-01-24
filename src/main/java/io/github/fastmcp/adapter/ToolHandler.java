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
 * - Build a BiFunction<Object, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>>
 * - Bind method arguments via ArgumentBinder
 * - Invoke the method reflectively
 * - If the tool is marked as async, unwrap the Mono/Flux and marshal results
 * - Convert exceptions into error CallToolResults
 */

import io.github.fastmcp.model.ToolMeta;
import io.github.fastmcp.adapter.ArgumentBinder;
import io.github.fastmcp.adapter.ResponseMarshaller;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.function.BiFunction;
import java.lang.reflect.Method;
import java.util.Map;

// Minimal tool handler adapter, wraps annotated method as a Reactor-based handler
public class ToolHandler {
    private final Object instance;
    private final ToolMeta meta;
    private final ArgumentBinder binder;
    private final ResponseMarshaller marshaller;

    public ToolHandler(Object instance, ToolMeta meta, ArgumentBinder binder, ResponseMarshaller marshaller) {
        this.instance = instance;
        this.meta = meta;
        this.binder = binder;
        this.marshaller = marshaller;
    }

    public BiFunction<Object, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> asHandler() {
        return (exchange, request) -> {
            try {
                Object[] args = binder.bind(meta.getMethod(), request.arguments());
                Object result = meta.getMethod().invoke(instance, args);

                if (meta.isAsync()) {
                    return ((Mono<?>) result).map(marshaller::marshal);
                } else {
                    return Mono.just(marshaller.marshal(result));
                }
            } catch (Exception e) {
                return Mono.just(errorResult(e));
            }
        };
    }

    private McpSchema.CallToolResult errorResult(Exception e) {
        return McpSchema.CallToolResult.builder()
            .content(List.of(new McpSchema.TextContent(e.getMessage())))
            .isError(true)
            .build();
    }
}
