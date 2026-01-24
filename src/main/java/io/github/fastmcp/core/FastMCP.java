package io.github.fastmcp.core;

import java.lang.reflect.Constructor;

public class FastMCP {
    private final Class<?> serverClass;
    private Object serverInstance;
    private TransportType transport = TransportType.STDIO;

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
        // Minimal skeleton: return a dummy async server
        return new DummyMcpAsyncServer();
    }

    private void instantiateServer() {
        try {
            Constructor<?> ctor = serverClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            serverInstance = ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate server", e);
        }
    }

    public void run() {
        McpAsyncServer server = build();
        // Block briefly in this stub; in real impl would block until termination
        server.awaitTermination();
    }

    // Simple placeholder transport enum
    public enum TransportType {
        STDIO, HTTP_SSE, HTTP_STREAMABLE
    }

    private static class DummyMcpAsyncServer implements McpAsyncServer {
        @Override
        public void awaitTermination() {
            // no-op for stub
        }
        @Override
        public void close() {
            // no-op for stub
        }
    }
}
