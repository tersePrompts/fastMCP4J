package com.ultrathink.fastmcp.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.ultrathink.fastmcp.notification.NotificationSender;
import com.ultrathink.fastmcp.openapi.OpenApiGenerator;
import com.ultrathink.fastmcp.scanner.AnnotationScanner;
import com.ultrathink.fastmcp.schema.SchemaGenerator;
import com.ultrathink.fastmcp.model.ServerMeta;
import com.ultrathink.fastmcp.model.ToolMeta;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * Main entry point for FastMCP4J framework.
 * Provides fluent API for building MCP servers from annotated classes.
 */
public class FastMCP {
    private final Class<?> serverClass;
    private Object serverInstance;
    private TransportType transport = TransportType.STDIO;
    private int port = 8080;
    private String mcpUri = "/mcp";
    private final AnnotationScanner scanner = new AnnotationScanner();
    private final SchemaGenerator schemaGenerator = new SchemaGenerator();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private NotificationSender notificationSender;
    private HttpServer httpServer;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

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

    public FastMCP port(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        this.port = port;
        return this;
    }

    public FastMCP mcpUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            throw new IllegalArgumentException("MCP URI cannot be null or empty");
        }
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        this.mcpUri = uri;
        return this;
    }

    public McpAsyncServer build() {
        instantiateServer();

        // Scan server metadata
        ServerMeta meta = scanner.scan(serverClass);

        // Create JSON mapper
        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);

        // Build based on transport type
        return switch (transport) {
            case STDIO -> buildStdioServer(meta, jsonMapper);
            case HTTP_STREAMABLE -> buildStreamableHttpServer(meta, jsonMapper);
            case HTTP_SSE -> throw new UnsupportedOperationException("SSE transport not yet implemented");
        };
    }

    private McpAsyncServer buildStdioServer(ServerMeta meta, JacksonMcpJsonMapper jsonMapper) {
        StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(jsonMapper);
        return buildMcpServer(meta, transportProvider);
    }

    private McpAsyncServer buildStreamableHttpServer(ServerMeta meta, JacksonMcpJsonMapper jsonMapper) {
        try {
            // Create simple HttpServer
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);

            // Health endpoint
            httpServer.createContext("/health", exchange -> {
                sendResponse(exchange, 200, "OK");
            });

            // MCP tools list endpoint
            httpServer.createContext(mcpUri + "/tools/list", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                    return;
                }

                List<McpSchema.Tool> tools = meta.getTools().stream()
                    .map(this::convertToMcpTool)
                    .toList();

                var response = Map.of("tools", tools);
                String json = objectMapper.writeValueAsString(response);
                sendJsonResponse(exchange, 200, json);
            });

            // MCP tool call endpoint
            httpServer.createContext(mcpUri + "/tools/call", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                    return;
                }

                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                @SuppressWarnings("unchecked")
                Map<String, Object> request = objectMapper.readValue(requestBody, Map.class);

                String toolName = (String) request.get("name");
                @SuppressWarnings("unchecked")
                Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");

                // Find and execute tool
                ToolMeta toolMeta = meta.getTools().stream()
                    .filter(t -> t.getName().equals(toolName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));

                var callRequest = new McpSchema.CallToolRequest(toolName, arguments);
                var handler = new com.ultrathink.fastmcp.adapter.ToolHandler(
                    serverInstance,
                    toolMeta,
                    new com.ultrathink.fastmcp.adapter.ArgumentBinder(),
                    new com.ultrathink.fastmcp.adapter.ResponseMarshaller()
                );

                McpSchema.CallToolResult result = handler.asHandler().apply(null, callRequest).block();
                String json = objectMapper.writeValueAsString(result);
                sendJsonResponse(exchange, 200, json);
            });

            httpServer.setExecutor(null);
            httpServer.start();

            // Build MCP server using STDIO transport (we're handling HTTP separately)
            return buildStdioServer(meta, jsonMapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start HTTP server", e);
        }
    }

    private McpSchema.Tool convertToMcpTool(ToolMeta toolMeta) {
        var schemaMap = schemaGenerator.generate(toolMeta.getMethod());
        String type = (String) schemaMap.get("type");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schemaMap.get("properties");
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schemaMap.get("required");

        var inputSchema = new McpSchema.JsonSchema(type, properties, required, null, null, null);

        return McpSchema.Tool.builder()
            .name(toolMeta.getName())
            .description(toolMeta.getDescription())
            .inputSchema(inputSchema)
            .build();
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, json.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes());
        }
    }

    private McpAsyncServer buildMcpServer(ServerMeta meta, StdioServerTransportProvider transportProvider) {
        var builder = McpServer.async(transportProvider)
            .serverInfo(meta.getName(), meta.getVersion())
            .capabilities(McpSchema.ServerCapabilities.builder()
                .tools(true)
                .logging()
                .build());

        // Register tools
        for (ToolMeta toolMeta : meta.getTools()) {
            builder.tools(buildToolSpec(toolMeta));
        }

        return builder.build();
    }

    private McpServerFeatures.AsyncToolSpecification buildToolSpec(ToolMeta toolMeta) {
        // Generate input schema
        var schemaMap = schemaGenerator.generate(toolMeta.getMethod());

        // Extract schema fields
        String type = (String) schemaMap.get("type");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schemaMap.get("properties");
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schemaMap.get("required");

        var inputSchema = new McpSchema.JsonSchema(
            type,
            properties,
            required,
            null,  // additionalProperties
            null,  // $defs
            null   // definitions
        );

        // Create tool definition
        var tool = McpSchema.Tool.builder()
            .name(toolMeta.getName())
            .description(toolMeta.getDescription())
            .inputSchema(inputSchema)
            .build();

        // Create tool handler using the new constructor (not deprecated)
        return new McpServerFeatures.AsyncToolSpecification(
            tool,
            null,  // deprecated call parameter
            (exchange, request) -> {
                // Use ToolHandler to invoke the method
                var handler = new com.ultrathink.fastmcp.adapter.ToolHandler(
                    serverInstance,
                    toolMeta,
                    new com.ultrathink.fastmcp.adapter.ArgumentBinder(),
                    new com.ultrathink.fastmcp.adapter.ResponseMarshaller()
                );
                return handler.asHandler().apply(exchange, request);
            }
        );
    }

    private void instantiateServer() {
        try {
            serverInstance = serverClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate server", e);
        }
    }

    public void run() {
        McpAsyncServer server = build();

        // Log server info
        ServerMeta meta = scanner.scan(serverClass);
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  FastMCP Server Started                                    ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║  Server: " + String.format("%-48s", meta.getName() + " v" + meta.getVersion()) + " ║");
        System.out.println("║  Transport: " + String.format("%-45s", transport) + " ║");
        System.out.println("║  Tools: " + String.format("%-49d", meta.getTools().size()) + " ║");

        if (transport == TransportType.HTTP_STREAMABLE || transport == TransportType.HTTP_SSE) {
            System.out.println("╠════════════════════════════════════════════════════════════╣");
            System.out.println("║  Endpoints:                                                ║");
            System.out.println("║    POST http://localhost:" + port + mcpUri + "/tools/list" + String.format("%" + (24 - mcpUri.length()) + "s", "") + " ║");
            System.out.println("║    POST http://localhost:" + port + mcpUri + "/tools/call" + String.format("%" + (24 - mcpUri.length()) + "s", "") + " ║");
            System.out.println("║    GET  http://localhost:" + port + "/health" + String.format("%" + (31 - mcpUri.length()) + "s", "") + " ║");
        }

        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutting down FastMCP server...");
            server.close();
            if (httpServer != null) {
                httpServer.stop(0);
            }
            shutdownLatch.countDown();
        }));

        // Block until shutdown
        try {
            if (httpServer != null) {
                shutdownLatch.await();
            } else {
                server.closeGracefully().block();
            }
        } catch (Exception e) {
            throw new RuntimeException("Server error", e);
        }
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
}
