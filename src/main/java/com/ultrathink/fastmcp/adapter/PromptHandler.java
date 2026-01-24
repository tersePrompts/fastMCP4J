package com.ultrathink.fastmcp.adapter;

import com.ultrathink.fastmcp.model.PromptMeta;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.function.BiFunction;

/**
 * PromptHandler
 * Bridges a PromptMeta (reflective method) to a Reactor-based MCP prompt handler.
 *
 * This class encapsulates how a server object's annotated prompt method is invoked
 * when a client requests the prompt. It wires together argument binding, method
 * invocation, and response marshalling for prompt messages.
 *
 * Key behavior:
 * - Build a BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>>
 * - Bind method arguments via ArgumentBinder
 * - Invoke the method reflectively
 * - If the prompt is marked as async, unwrap the Mono/Flux and marshal results
 * - Convert exceptions into error GetPromptResults
 */

public class PromptHandler {
    private final Object instance;
    private final PromptMeta meta;
    private final ArgumentBinder binder;
    private final PromptResponseMarshaller marshaller;

    public PromptHandler(Object instance, PromptMeta meta, ArgumentBinder binder, PromptResponseMarshaller marshaller) {
        this.instance = instance;
        this.meta = meta;
        this.binder = binder;
        this.marshaller = marshaller;
    }

    public BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> asHandler() {
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

    private McpSchema.GetPromptResult errorResult(Exception e) {
        return new McpSchema.GetPromptResult(
            "Error occurred while generating prompt: " + e.getMessage(),
            List.of(new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent("Error: " + e.getMessage())
            ))
        );
    }
}