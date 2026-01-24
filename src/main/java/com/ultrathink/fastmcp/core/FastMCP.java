package com.ultrathink.fastmcp.core;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Main entry point for FastMCP4J framework.
 * Provides fluent API for building MCP servers from annotated classes.
 */
public class FastMCP {
    private final Class<?> serverClass;
    private Object serverInstance;
    private TransportType transport = TransportType.STDIO;
    private final AnnotationScanner scanner = new AnnotationScanner();
    private final SchemaGenerator schemaGenerator = new SchemaGenerator();
    private NotificationSender notificationSender;

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

        // Scan server metadata
        ServerMeta meta = scanner.scan(serverClass);

        // Create transport provider
        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(jsonMapper);

        // Build server using MCP SDK builder
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

        System.out.println("FastMCP server starting...");
        System.out.println("Transport: " + transport);
        System.out.println("Tools: " + scanner.scan(serverClass).getTools().size());

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down FastMCP server...");
            server.close();
        }));

        // Block until shutdown
        server.closeGracefully().block();
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
