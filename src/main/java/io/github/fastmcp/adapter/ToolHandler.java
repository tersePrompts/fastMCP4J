package io.github.fastmcp.adapter;

import io.github.fastmcp.model.ToolMeta;
import io.github.fastmcp.adapter.ArgumentBinder;
import io.github.fastmcp.adapter.ResponseMarshaller;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.function.BiFunction;
import java.lang.reflect.Method;
import java.util.Map;

import io.modelcontextprotocol.sdk.*; // MCP SDK common types, may be resolved by classpath

// Minimal tool handler adapter, wraps annotated method as a Reactor-based handler
public class ToolHandler {
    Object instance;
    ToolMeta meta;
    ArgumentBinder binder;
    ResponseMarshaller marshaller;

    public BiFunction<McpAsyncServerExchange, CallToolRequest, Mono<CallToolResult>> asHandler() {
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

    private CallToolResult errorResult(Exception e) {
        return CallToolResult.builder()
            .content(List.of(new TextContent(e.getMessage())))
            .isError(true)
            .build();
    }
}
