# CHUNK 10: Resource and Prompt Handlers

**Dependencies**: CHUNK 2 (Model), CHUNK 4 (SchemaGen), CHUNK 5 (ArgumentBinder), CHUNK 6 (FastMCP core)

**Files**:
- `src/main/java/io/github/fastmcp/adapter/ResourceHandler.java`
- `src/main/java/io/github/fastmcp/adapter/PromptHandler.java`
- `src/test/java/io/github/fastmcp/adapter/AdapterTest.java` (extend with new tests)

---

## Implementation

### ResourceHandler.java
```java
package io.github.fastmcp.adapter;

import io.github.fastmcp.model.ResourceMeta;
import io.modelcontextprotocol.sdk.ReadResourceResult;
import io.modelcontextprotocol.sdk.ResourceContent;
import io.modelcontextprotocol.sdk.McpAsyncServerExchange;
import io.modelcontextprotocol.sdk.ReadResourceRequest;
import reactor.core.publisher.Mono;
import java.util.List;

public class ResourceHandler {
    private final Object instance;
    private final ResourceMeta meta;
    private final ResponseMarshaller marshaller;

    public ResourceHandler(Object instance, ResourceMeta meta, ResponseMarshaller marshaller) {
        this.instance = instance;
        this.meta = meta;
        this.marshaller = marshaller;
    }

    public java.util.function.BiFunction<McpAsyncServerExchange, ReadResourceRequest, Mono<ReadResourceResult>> asHandler() {
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
        CallToolResult textResult = marshaller.marshal(value);
        String content = textResult.getContent().isEmpty() ? "" : textResult.getContent().get(0).getText();

        ResourceContent resourceContent = new ResourceContent(
            meta.getMimeType(),
            content
        );

        return ReadResourceResult.builder()
            .contents(List.of(resourceContent))
            .build();
    }

    private ReadResourceResult errorResult(Exception e) {
        ResourceContent errorContent = new ResourceContent(
            "text/plain",
            "Error: " + e.getMessage()
        );

        return ReadResourceResult.builder()
            .contents(List.of(errorContent))
            .build();
    }
}
```

### PromptHandler.java
```java
package io.github.fastmcp.adapter;

import io.github.fastmcp.model.PromptMeta;
import io.modelcontextprotocol.sdk.GetPromptResult;
import io.modelcontextprotocol.sdk.PromptMessage;
import io.modelcontextprotocol.sdk.McpAsyncServerExchange;
import io.modelcontextprotocol.sdk.GetPromptRequest;
import reactor.core.publisher.Mono;
import java.util.List;

public class PromptHandler {
    private final Object instance;
    private final PromptMeta meta;

    public PromptHandler(Object instance, PromptMeta meta) {
        this.instance = instance;
        this.meta = meta;
    }

    public java.util.function.BiFunction<McpAsyncServerExchange, GetPromptRequest, Mono<GetPromptResult>> asHandler() {
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
        String text = value instanceof String ? (String) value : value.toString();

        PromptMessage message = new PromptMessage(
            "user",
            text
        );

        return GetPromptResult.builder()
            .messages(List.of(message))
            .build();
    }

    private GetPromptResult errorResult(Exception e) {
        PromptMessage error = new PromptMessage(
            "user",
            "Error: " + e.getMessage()
        );

        return GetPromptResult.builder()
            .messages(List.of(error))
            .build();
    }
}
```

---

## Tests

Extend `AdapterTest.java`:
```java
@McpServer(name = "TestServer")
public static class TestServer {
    @McpTool(description = "Echo tool")
    public String echo(String message) {
        return "echo: " + message;
    }

    @McpResource(uri = "file://config.txt", description = "Config file")
    public String getConfig() {
        return "config: value";
    }

    @McpPrompt(description = "System prompt")
    public String getSystemPrompt() {
        return "You are a helpful assistant.";
    }
}

@Test
void testResourceHandlerSyncInvocation() throws Exception {
    TestServer server = new TestServer();
    AnnotationScanner scanner = new AnnotationScanner();
    ServerMeta meta = scanner.scan(TestServer.class);

    ResourceMeta resourceMeta = meta.getResources().get(0);

    ResourceHandler handler = new ResourceHandler(
        server,
        resourceMeta,
        new ResponseMarshaller()
    );

    McpAsyncServerExchange exchange = new McpAsyncServerExchange() {};
    ReadResourceRequest request = new ReadResourceRequest("file://config.txt");

    Mono<ReadResourceResult> resultMono = handler.asHandler().apply(exchange, request);

    ReadResourceResult result = resultMono.block();
    assertNotNull(result);
    assertFalse(result.getContents().isEmpty());
    assertEquals("config: value", result.getContents().get(0).getText());
}

@Test
void testPromptHandlerSyncInvocation() throws Exception {
    TestServer server = new TestServer();
    AnnotationScanner scanner = new AnnotationScanner();
    ServerMeta meta = scanner.scan(TestServer.class);

    PromptMeta promptMeta = meta.getPrompts().get(0);

    PromptHandler handler = new PromptHandler(
        server,
        promptMeta
    );

    McpAsyncServerExchange exchange = new McpAsyncServerExchange() {};
    GetPromptRequest request = new GetPromptRequest("systemPrompt");

    Mono<GetPromptResult> resultMono = handler.asHandler().apply(exchange, request);

    GetPromptResult result = resultMono.block();
    assertNotNull(result);
    assertFalse(result.getMessages().isEmpty());
    assertEquals("You are a helpful assistant.", result.getMessages().get(0).getContent().getText());
}
```

---

## Verification
- [ ] ResourceHandler handles sync methods correctly
- [ ] ResourceHandler handles async methods correctly
- [ ] PromptHandler handles sync methods correctly
- [ ] PromptHandler handles async methods correctly
- [ ] Error handling works for both handlers
- [ ] Tests pass
