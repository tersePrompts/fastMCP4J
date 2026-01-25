package io.github.fastmcp.core;

import io.github.fastmcp.annotations.*;
import io.github.fastmcp.scanner.AnnotationScanner;
import io.github.fastmcp.schema.SchemaGenerator;
import io.github.fastmcp.adapter.ToolHandler;
import io.github.fastmcp.adapter.ArgumentBinder;
import io.github.fastmcp.adapter.ResponseMarshaller;
import io.github.fastmcp.adapter.ProgressAwareToolHandler;
import io.github.fastmcp.model.ServerMeta;
import io.github.fastmcp.model.ToolMeta;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import io.github.fastmcp.openapi.OpenApiGenerator;
import io.github.fastmcp.notification.NotificationSender;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Consumer;

public class FastMCP {
    private final Class<?> serverClass;
    private Object serverInstance;
    private TransportType transport = TransportType.STDIO;
    private final AnnotationScanner scanner = new AnnotationScanner();
    private final SchemaGenerator schemaGenerator = new SchemaGenerator();
    private final ObjectMapper mapper = new ObjectMapper();
    private NotificationSender notificationSender;

    private FastMCP(Class<?> serverClass) {
        this.serverClass = serverClass;
    }

    public static FastMCP server(Class<?> clazz) {
        return new FastMCP(clazz);
    }

    public FastMCP stdio() { this.transport = TransportType.STDIO; return this; }
    public FastMCP sse() { this.transport = TransportType.HTTP_SSE; return this; }
    public FastMCP streamable() { this.transport = TransportType.HTTP_STREAMABLE; return this; }

    public McpAsyncServer build() {
        instantiateServer();
        return new RealMcpAsyncServer();
    }

    private void instantiateServer() {
        try {
            serverInstance = serverClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate server", e);
        }
    }

    public void run() {
        build().awaitTermination();
    }

    public FastMCP withNotificationSender(Consumer<Object> sendFunction) {
        this.notificationSender = new NotificationSender(sendFunction);
        return this;
    }

    public NotificationSender getNotificationSender() {
        return notificationSender;
    }

    public String generateOpenApi() {
        OpenApiGenerator generator = new OpenApiGenerator();
        ServerMeta meta = scanner.scan(serverClass);
        return generator.toJson(meta);
    }

    public void generateOpenApiFile(String outputPath) {
        String json = generateOpenApi();
        try {
            java.nio.file.Files.writeString(
                java.nio.file.Path.of(outputPath),
                json
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to write OpenAPI file", e);
        }
    }

    private class RealMcpAsyncServer implements McpAsyncServer {
        private ServerMeta meta;

        RealMcpAsyncServer() {
            this.meta = scanner.scan(serverClass);
        }

        @Override
        public void awaitTermination() {
            // TODO: Implement actual server with MCP SDK
            // For now, this is a placeholder that demonstrates the architecture
            System.out.println("FastMCP server started with " + meta.getTools().size() + " tools");
            System.out.println("Tools: " + meta.getTools().stream().map(ToolMeta::getName).toList());
        }

        @Override
        public void close() {
            // TODO: Implement graceful shutdown
        }

        private Object createHandlerForTool(ToolMeta toolMeta) {
            ArgumentBinder binder = new ArgumentBinder();
            ResponseMarshaller marshaller = new ResponseMarshaller();
            
            if (toolMeta.isProgressEnabled() && notificationSender != null) {
                return new ProgressAwareToolHandler(
                    serverInstance,
                    toolMeta,
                    binder,
                    marshaller,
                    notificationSender
                ).asHandler();
            } else {
                return new ToolHandler(
                    serverInstance,
                    toolMeta,
                    binder,
                    marshaller
                ).asHandler();
            }
        }
    }
}
