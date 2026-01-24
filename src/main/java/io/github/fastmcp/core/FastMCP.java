package io.github.fastmcp.core;

import io.github.fastmcp.annotations.*;
import io.github.fastmcp.scanner.AnnotationScanner;
import io.github.fastmcp.schema.SchemaGenerator;
import io.github.fastmcp.adapter.ToolHandler;
import io.github.fastmcp.adapter.ArgumentBinder;
import io.github.fastmcp.adapter.ResponseMarshaller;
import io.github.fastmcp.model.ServerMeta;
import io.github.fastmcp.model.ToolMeta;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Map;

public class FastMCP {
    private final Class<?> serverClass;
    private Object serverInstance;
    private TransportType transport = TransportType.STDIO;
    private final AnnotationScanner scanner = new AnnotationScanner();
    private final SchemaGenerator schemaGenerator = new SchemaGenerator();
    private final ObjectMapper mapper = new ObjectMapper();

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
            ToolHandler handler = new ToolHandler();
            handler.instance = serverInstance;
            handler.meta = toolMeta;
            handler.binder = new ArgumentBinder();
            handler.marshaller = new ResponseMarshaller();
            return handler.asHandler();
        }
    }
}
