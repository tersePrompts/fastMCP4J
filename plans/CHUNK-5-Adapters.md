# CHUNK 5: Adapters

**Dependencies**: CHUNK 2 (Model), CHUNK 0 (Exception)

**Files**:
- `src/main/java/io/github/fastmcp/adapter/ToolHandler.java`
- `src/main/java/io/github/fastmcp/adapter/ResourceHandler.java`
- `src/main/java/io/github/fastmcp/adapter/PromptHandler.java`
- `src/main/java/io/github/fastmcp/adapter/ArgumentBinder.java`
- `src/main/java/io/github/fastmcp/adapter/ResponseMarshaller.java`
- `src/test/java/io/github/fastmcp/adapter/AdapterTest.java`

---

## Implementation

### ArgumentBinder.java

```java
package com.ultrathink.fastmcp.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import exception.com.ultrathink.fastmcp.FastMcpException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

public class ArgumentBinder {
    private final ObjectMapper mapper = new ObjectMapper();

    public Object[] bind(Method method, Map<String, Object> args) {
        Parameter[] params = method.getParameters();
        Object[] bound = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            String name = getParamName(params[i]);
            Object raw = args.get(name);
            bound[i] = mapper.convertValue(raw, params[i].getType());
        }

        return bound;
    }

    private String getParamName(Parameter p) {
        JsonProperty ann = p.getAnnotation(JsonProperty.class);
        if (ann != null && !ann.value().isEmpty()) {
            return ann.value();
        }

        if (p.isNamePresent()) {
            return p.getName();
        }

        throw new FastMcpException(
                "Cannot determine parameter name. Use @JsonProperty or compile with -parameters"
        );
    }
}
```

### ResponseMarshaller.java

```java
package com.ultrathink.fastmcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import exception.com.ultrathink.fastmcp.FastMcpException;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import java.util.List;

public class ResponseMarshaller {
    private final ObjectMapper mapper = new ObjectMapper();

    public CallToolResult marshal(Object value) {
        if (value == null) {
            return emptyResult();
        }

        String text = switch (value) {
            case String s -> s;
            case Number n -> n.toString();
            case Boolean b -> b.toString();
            default -> {
                try {
                    yield mapper.writeValueAsString(value);
                } catch (Exception e) {
                    throw new FastMcpException("Failed to marshal response", e);
                }
            }
        };

        return CallToolResult.builder()
                .content(List.of(new TextContent(text)))
                .isError(false)
                .build();
    }

    private CallToolResult emptyResult() {
        return CallToolResult.builder()
                .content(List.of())
                .isError(false)
                .build();
    }
}
```

### ToolHandler.java

```java
package com.ultrathink.fastmcp.adapter;

import com.ultrathink.fastmcp.adapter.ArgumentBinder;
import com.ultrathink.fastmcp.adapter.ResponseMarshaller;
import com.ultrathink.fastmcp.model.ToolMeta;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpTransport.McpAsyncServerExchange;
import lombok.Value;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;

@Value
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
```

### ResourceHandler.java

```java
package com.ultrathink.fastmcp.adapter;

import com.ultrathink.fastmcp.adapter.ArgumentBinder;
import com.ultrathink.fastmcp.model.ResourceMeta;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpTransport.McpAsyncServerExchange;
import lombok.Value;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;

@Value
public class ResourceHandler {
    Object instance;
    ResourceMeta meta;
    ArgumentBinder binder;

    public BiFunction<McpAsyncServerExchange, ReadResourceRequest, Mono<ReadResourceResult>> asHandler() {
        return (exchange, request) -> {
            try {
                Object result = meta.getMethod().invoke(instance);
                String text = result != null ? result.toString() : "";

                ReadResourceResult response = ReadResourceResult.builder()
                        .contents(List.of(new TextContent(text)))
                        .build();

                if (meta.isAsync()) {
                    return ((Mono<?>) result).map(v -> response);
                } else {
                    return Mono.just(response);
                }
            } catch (Exception e) {
                return Mono.error(e);
            }
        };
    }
}
```

### PromptHandler.java

```java
package com.ultrathink.fastmcp.adapter;

import com.ultrathink.fastmcp.adapter.ArgumentBinder;
import com.ultrathink.fastmcp.model.PromptMeta;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpTransport.McpAsyncServerExchange;
import lombok.Value;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

@Value
public class PromptHandler {
    Object instance;
    PromptMeta meta;
    ArgumentBinder binder;

    public BiFunction<McpAsyncServerExchange, GetPromptRequest, Mono<GetPromptResult>> asHandler() {
        return (exchange, request) -> {
            return Mono.error(new UnsupportedOperationException("Prompt handler not yet implemented"));
        };
    }
}
```

---

## Tests

### AdapterTest.java

```java
package com.ultrathink.fastmcp.adapter;

import com.ultrathink.fastmcp.adapter.ArgumentBinder;
import com.ultrathink.fastmcp.adapter.ResponseMarshaller;
import com.ultrathink.fastmcp.adapter.ToolHandler;
import com.ultrathink.fastmcp.model.ToolMeta;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdapterTest {

    public static class TestServer {
        public int add(int a, int b) {
            return a + b;
        }
    }

    @Test
    void testToolHandlerSyncInvocation() throws Exception {
        TestServer instance = new TestServer();
        Method method = TestServer.class.getMethod("add", int.class, int.class);
        ToolMeta meta = new ToolMeta("add", "Add numbers", method, false);

        ToolHandler handler = new ToolHandler(
                instance,
                meta,
                new ArgumentBinder(),
                new ResponseMarshaller()
        );

        CallToolRequest request = CallToolRequest.builder()
                .name("add")
                .arguments(Map.of("a", 3, "b", 5))
                .build();

        Mono<CallToolResult> result = handler.asHandler().apply(null, request);
        CallToolResult res = result.block();

        assertNotNull(res);
        assertFalse(res.isError());
        assertEquals("8", res.content().get(0).text());
    }

    @Test
    void testArgumentBinding() throws Exception {
        ArgumentBinder binder = new ArgumentBinder();
        Method method = TestServer.class.getMethod("add", int.class, int.class);

        Object[] args = binder.bind(method, Map.of("a", 10, "b", 20));

        assertEquals(2, args.length);
        assertEquals(10, args[0]);
        assertEquals(20, args[1]);
    }

    @Test
    void testResponseMarshalling() {
        ResponseMarshaller marshaller = new ResponseMarshaller();

        CallToolResult result = marshaller.marshal(42);
        assertEquals("42", result.content().get(0).text());
        assertFalse(result.isError());
    }
}
```

---

## Verification
- [ ] ToolHandler wraps methods correctly
- [ ] ArgumentBinder converts types
- [ ] ResponseMarshaller handles all types
- [ ] Error handling works
- [ ] Sync and async execution paths work
