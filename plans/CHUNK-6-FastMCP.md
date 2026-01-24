# CHUNK 6: FastMCP Core

**Dependencies**: ALL previous chunks (0-5)

**Files**:
- `src/main/java/io/github/fastmcp/core/FastMCP.java`
- `src/main/java/io/github/fastmcp/core/TransportType.java`
- `src/test/java/io/github/fastmcp/core/FastMCPTest.java`

---

## Implementation

### TransportType.java
```java
package io.github.fastmcp.core;

public enum TransportType {
    STDIO,
    HTTP_SSE,
    HTTP_STREAMABLE
}
```

### FastMCP.java

```java
package io.github.fastmcp.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultrathink.fastmcp.adapter.ArgumentBinder;
import com.ultrathink.fastmcp.adapter.ResponseMarshaller;
import com.ultrathink.fastmcp.adapter.ToolHandler;
import com.ultrathink.fastmcp.core.McpAsyncServer;
import com.ultrathink.fastmcp.core.TransportType;
import com.ultrathink.fastmcp.model.PromptMeta;
import com.ultrathink.fastmcp.model.ResourceMeta;
import com.ultrathink.fastmcp.model.ServerMeta;
import com.ultrathink.fastmcp.model.ToolMeta;
import exception.com.ultrathink.fastmcp.FastMcpException;
import scanner.com.ultrathink.fastmcp.AnnotationScanner;
import schema.com.ultrathink.fastmcp.SchemaGenerator;
import io.modelcontextprotocol.sdk.server.McpServer;
import io.modelcontextprotocol.sdk.server.McpAsyncServer;
import io.modelcontextprotocol.sdk.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.sdk.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.sdk.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.*;
import io.modelcontextprotocol.spec.McpTransport.McpServerTransportProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class FastMCP {
    private final Class<?> serverClass;
    private Object serverInstance;
    private TransportType transport = TransportType.STDIO;
    private final AnnotationScanner scanner = new AnnotationScanner();
    private final SchemaGenerator schemaGen = new SchemaGenerator();

    private FastMCP(Class<?> serverClass) {
        this.serverClass = serverClass;
    }

    public static FastMCP server(Class<?> clazz) {
        return new FastMCP(clazz);
    }

    public FastMCP stdio() {
        this.transport = TransportType.STDIO;
        return this;
    }

    public FastMCP sse() {
        this.transport = TransportType.HTTP_SSE;
        return this;
    }

    public FastMCP streamable() {
        this.transport = TransportType.HTTP_STREAMABLE;
        return this;
    }

    public McpAsyncServer build() {
        instantiateServer();
        ServerMeta meta = scanner.scan(serverClass);

        McpServerTransportProvider provider = createTransport();
        McpServer.AsyncSpecification spec = McpServer.async(provider)
                .serverInfo(meta.getName(), meta.getVersion());

        if (!meta.getInstructions().isEmpty()) {
            spec.instructions(meta.getInstructions());
        }

        for (ToolMeta tool : meta.getTools()) {
            registerTool(spec, tool);
        }

        for (ResourceMeta res : meta.getResources()) {
            registerResource(spec, res);
        }

        for (PromptMeta prompt : meta.getPrompts()) {
            registerPrompt(spec, prompt);
        }

        return spec.build();
    }

    private void registerTool(McpServer.AsyncSpecification spec, ToolMeta toolMeta) {
        Map<String, Object> schema = schemaGen.generate(toolMeta.getMethod());

        Tool tool = Tool.builder()
                .name(toolMeta.getName())
                .description(toolMeta.getDescription())
                .inputSchema(schema)
                .build();

        ToolHandler handler = new ToolHandler(
                serverInstance,
                toolMeta,
                new ArgumentBinder(),
                new ResponseMarshaller()
        );

        spec.tool(tool, handler.asHandler());
    }

    private void registerResource(McpServer.AsyncSpecification spec, ResourceMeta resMeta) {
        Resource resource = Resource.builder()
                .uri(resMeta.getUri())
                .name(resMeta.getName())
                .description(resMeta.getDescription())
                .mimeType(resMeta.getMimeType())
                .build();

        ResourceHandler handler = new ResourceHandler(
                serverInstance,
                resMeta,
                new ArgumentBinder()
        );

        spec.resource(resource, handler.asHandler());
    }

    private void registerPrompt(McpServer.AsyncSpecification spec, PromptMeta promptMeta) {
        Prompt prompt = Prompt.builder()
                .name(promptMeta.getName())
                .description(promptMeta.getDescription())
                .build();

        PromptHandler handler = new PromptHandler(
                serverInstance,
                promptMeta,
                new ArgumentBinder()
        );

        spec.prompt(prompt, handler.asHandler());
    }

    private McpServerTransportProvider createTransport() {
        ObjectMapper mapper = new ObjectMapper();
        return switch (transport) {
            case STDIO -> new StdioServerTransportProvider(mapper);
            case HTTP_SSE -> new HttpServletSseServerTransportProvider(mapper);
            case HTTP_STREAMABLE -> new HttpServletStreamableServerTransportProvider(mapper);
        };
    }

    private void instantiateServer() {
        try {
            serverInstance = serverClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new FastMcpException("Failed to instantiate server", e);
        }
    }

    public void run() {
        McpAsyncServer server = build();
        log.info("FastMCP server started: {}", serverClass.getSimpleName());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down");
            server.close();
        }));

        server.awaitTermination();
    }
}
```

---

## Tests

### FastMCPTest.java

```java
package io.github.fastmcp.core;

import com.ultrathink.fastmcp.core.FastMCP;
import com.ultrathink.fastmcp.core.McpAsyncServer;
import com.ultrathink.fastmcp.annotations.McpServer;
import annotations.com.ultrathink.fastmcp.McpTool;
import io.modelcontextprotocol.sdk.server.McpAsyncServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FastMCPTest {

    @McpServer(name = "TestServer", version = "1.0.0")
    public static class TestServer {
        @McpTool(description = "Add numbers")
        public int add(int a, int b) {
            return a + b;
        }
    }

    @Test
    void testBuildServerFromClass() {
        McpAsyncServer server = FastMCP.server(TestServer.class)
                .stdio()
                .build();

        assertNotNull(server);
    }

    @Test
    void testStdioTransport() {
        FastMCP fastMcp = FastMCP.server(TestServer.class).stdio();
        assertNotNull(fastMcp);
    }

    @Test
    void testSseTransport() {
        FastMCP fastMcp = FastMCP.server(TestServer.class).sse();
        assertNotNull(fastMcp);
    }
}
```

---

## Verification
- [ ] FastMCP builds server successfully
- [ ] All transport types work
- [ ] Tools, resources, prompts registered
- [ ] Server starts and shuts down cleanly
- [ ] Integration test with real MCP client passes
