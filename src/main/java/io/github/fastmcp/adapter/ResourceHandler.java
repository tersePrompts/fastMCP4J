package io.github.fastmcp.adapter;

import io.github.fastmcp.model.ResourceMeta;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.function.BiFunction;

public class ResourceHandler {
    private final Object instance;
    private final ResourceMeta meta;
    private final ResponseMarshaller marshaller;

    public ResourceHandler(Object instance, ResourceMeta meta, ResponseMarshaller marshaller) {
        this.instance = instance;
        this.meta = meta;
        this.marshaller = marshaller;
    }

    public BiFunction<McpAsyncServerExchange, ReadResourceRequest, Mono<ReadResourceResult>> asHandler() {
        return (exchange, request) -> {
            try {
                Object result = meta.getMethod().invoke(instance);

                if (meta.isAsync()) {
                    return ((Mono<?>) result).map(this::toResourceResult);
                } else {
                    return Mono.just(toResourceResult(result));
                }
            } catch (Exception e) {
                return Mono.just(errorResult(e));
            }
        };
    }

    private ReadResourceResult toResourceResult(Object value) {
        String content;
        if (value == null) {
            content = "";
        } else {
            content = value.toString();
        }

        TextResourceContents resourceContent = new TextResourceContents(
            meta.getMimeType(),
            meta.getUri(),
            content
        );

        return new ReadResourceResult(List.of(resourceContent));
    }

    private ReadResourceResult errorResult(Exception e) {
        TextResourceContents errorContent = new TextResourceContents(
            "text/plain",
            meta.getUri(),
            "Error: " + e.getMessage()
        );

        return new ReadResourceResult(List.of(errorContent));
    }
}
