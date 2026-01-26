package com.ultrathink.fastmcp.adapter;

import com.ultrathink.fastmcp.model.ResourceMeta;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * ResourceHandler
 * Bridges a ResourceMeta (reflective method) to a Reactor-based MCP resource handler.
 *
 * This class encapsulates how a server object's annotated resource method is invoked
 * when a client requests the resource. It wires together argument binding, method
 * invocation, and response marshalling for resource content.
 *
 * Key behavior:
 * - Build a BiFunction&lt;McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono&lt;McpSchema.ReadResourceResult&gt;&gt;
 * - Bind method arguments via ArgumentBinder
 * - Invoke the method reflectively
 * - If the resource is marked as async, unwrap the Mono/Flux and marshal results
 * - Convert exceptions into error ReadResourceResults
 */

public class ResourceHandler {
    private final Object instance;
    private final ResourceMeta meta;
    private final ArgumentBinder binder;
    private final ResourceResponseMarshaller marshaller;

    public ResourceHandler(Object instance, ResourceMeta meta, ArgumentBinder binder, ResourceResponseMarshaller marshaller) {
        this.instance = instance;
        this.meta = meta;
        this.binder = binder;
        this.marshaller = marshaller;
    }

    public BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> asHandler() {
        return (exchange, request) -> {
            try {
                // ReadResourceRequest doesn't have arguments, use empty map
                Object[] args = binder.bind(meta.getMethod(), Map.of());
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

    private McpSchema.ReadResourceResult errorResult(Exception e) {
        return new McpSchema.ReadResourceResult(List.of(
            new McpSchema.BlobResourceContents(
                "text/plain", 
                null, 
                java.util.Base64.getEncoder().encodeToString(("Error: " + e.getMessage()).getBytes())
            )
        ));
    }
}