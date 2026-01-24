package io.github.fastmcp.adapter;

import io.github.fastmcp.model.PromptMeta;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Role;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.function.BiFunction;

public class PromptHandler {
    private final Object instance;
    private final PromptMeta meta;

    public PromptHandler(Object instance, PromptMeta meta) {
        this.instance = instance;
        this.meta = meta;
    }

    public BiFunction<McpAsyncServerExchange, GetPromptRequest, Mono<GetPromptResult>> asHandler() {
        return (exchange, request) -> {
            try {
                Object result = meta.getMethod().invoke(instance);

                if (meta.isAsync()) {
                    return ((Mono<?>) result).map(this::toPromptResult);
                } else {
                    return Mono.just(toPromptResult(result));
                }
            } catch (Exception e) {
                return Mono.just(errorResult(e));
            }
        };
    }

    private GetPromptResult toPromptResult(Object value) {
        String text;
        if (value == null) {
            text = "";
        } else if (value instanceof String s) {
            text = s;
        } else {
            text = value.toString();
        }

        PromptMessage message = new PromptMessage(
            Role.USER,
            new TextContent(text)
        );

        return new GetPromptResult("", List.of(message));
    }

    private GetPromptResult errorResult(Exception e) {
        PromptMessage error = new PromptMessage(
            Role.USER,
            new TextContent("Error: " + e.getMessage())
        );

        return new GetPromptResult("", List.of(error));
    }
}
