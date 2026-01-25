package com.ultrathink.fastmcp.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultrathink.fastmcp.adapter.*;
import com.ultrathink.fastmcp.annotations.McpMemory;
import com.ultrathink.fastmcp.annotations.McpTodo;
import com.ultrathink.fastmcp.annotations.McpPlanner;
import com.ultrathink.fastmcp.hook.HookManager;
import com.ultrathink.fastmcp.memory.InMemoryMemoryStore;
import com.ultrathink.fastmcp.memory.MemoryStore;
import com.ultrathink.fastmcp.memory.MemoryTool;
import com.ultrathink.fastmcp.planner.InMemoryPlanStore;
import com.ultrathink.fastmcp.planner.PlanStore;
import com.ultrathink.fastmcp.planner.PlannerTool;
import com.ultrathink.fastmcp.todo.InMemoryTodoStore;
import com.ultrathink.fastmcp.todo.TodoStore;
import com.ultrathink.fastmcp.todo.TodoTool;
import com.ultrathink.fastmcp.model.*;
import com.ultrathink.fastmcp.scanner.AnnotationScanner;
import com.ultrathink.fastmcp.schema.SchemaGenerator;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServer.AsyncSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.*;
import io.modelcontextprotocol.spec.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FastMCP {
    private final Class<?> serverClass;
    private final AnnotationScanner scanner = new AnnotationScanner();
    private final SchemaGenerator schemaGenerator = new SchemaGenerator();
    private String serverName;

    private TransportType transport = TransportType.STDIO;
    private int port = 8080;
    private String mcpUri = "/mcp";
    private Server jetty;
    
    private MemoryStore memoryStore;
    private TodoStore todoStore;
    private PlanStore planStore;

    private FastMCP(Class<?> serverClass) { this.serverClass = serverClass; }
    public static FastMCP server(Class<?> clazz) { return new FastMCP(clazz); }

    // Fluent API
    public FastMCP stdio() { this.transport = TransportType.STDIO; return this; }
    public FastMCP sse() { this.transport = TransportType.HTTP_SSE; return this; }
    public FastMCP streamable() { this.transport = TransportType.HTTP_STREAMABLE; return this; }
    public FastMCP port(int p) { this.port = p; return this; }
    public FastMCP mcpUri(String u) { this.mcpUri = u.startsWith("/") ? u : "/" + u; return this; }
    
    /**
     * Set a custom MemoryStore implementation.
     * Default is InMemoryMemoryStore.
     */
    public FastMCP memoryStore(MemoryStore store) { this.memoryStore = store; return this; }
    
    /**
     * Set a custom TodoStore implementation.
     * Default is InMemoryTodoStore.
     */
    public FastMCP todoStore(TodoStore store) { this.todoStore = store; return this; }
    
    /**
     * Set a custom PlanStore implementation.
     * Default is InMemoryPlanStore.
     */
    public FastMCP planStore(PlanStore store) { this.planStore = store; return this; }

    public void run() {
        try {
            McpAsyncServer mcp = build();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                mcp.closeGracefully().block(Duration.ofSeconds(10));
                if (jetty != null) try { jetty.stop(); } catch (Exception ignored) {}
            }));

            if (jetty != null) {
                jetty.join();
            } else {
                Thread.currentThread().join();
            }
        } catch (Exception e) { throw new RuntimeException("FastMCP Startup Failed", e); }
    }

    private McpAsyncServer build() throws Exception {
        Object instance = serverClass.getDeclaredConstructor().newInstance();
        ServerMeta meta = scanner.scan(serverClass);
        serverName = meta.getName(); // Set server name for context

        // Create HookManager for pre/post hooks
        HookManager hookManager = new HookManager(instance, meta.getTools());

        var mapper = new JacksonMcpJsonMapper(new ObjectMapper());

        io.modelcontextprotocol.server.McpServer.AsyncSpecification<?> builder = switch (transport) {
            case STDIO -> io.modelcontextprotocol.server.McpServer.async(new StdioServerTransportProvider(mapper));

            case HTTP_SSE -> {
                var p = HttpServletSseServerTransportProvider.builder()
                        .jsonMapper(mapper).messageEndpoint(mcpUri)
                        //here the endpoint is - /mcp/sse
                        .contextExtractor(FastMCP::extractContext).build();
                startJetty(p);
                yield io.modelcontextprotocol.server.McpServer.async(p);
            }

            case HTTP_STREAMABLE -> {
                var p = HttpServletStreamableServerTransportProvider.builder()
                        .jsonMapper(mapper)
                        .contextExtractor(FastMCP::extractContext)
                        .keepAliveInterval(Duration.ofSeconds(30))
                        .build();
                startJetty(p);
                yield io.modelcontextprotocol.server.McpServer.async(p);
            }
        };

        builder.serverInfo(meta.getName(), meta.getVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .resources(true, false)  // resources enabled, no list changes
                    .prompts(true)           // prompts enabled
                    .logging()
                    .completions()
                    .build());

        meta.getTools().forEach(t -> builder.tools(buildTool(t, instance, hookManager)));
        meta.getResources().forEach(r -> builder.resources(buildResource(r, instance)));
        meta.getPrompts().forEach(p -> builder.prompts(buildPrompt(p, instance)));

        // Register memory tool if @McpMemory annotation is present
        if (serverClass.isAnnotationPresent(McpMemory.class)) {
            MemoryStore store = memoryStore != null ? memoryStore : new InMemoryMemoryStore();
            MemoryTool memoryTool = new MemoryTool(store);
            String toolSchema = memoryTool.getToolSchema();
            Map<String, Object> schemaMap = (Map<String, Object>) parseJsonSchema(toolSchema);

            var tool = McpSchema.Tool.builder()
                    .name("memory")
                    .description("Memory tool for persistent storage and retrieval of information")
                    .inputSchema(new McpSchema.JsonSchema(
                        (String) schemaMap.getOrDefault("type", "object"),
                        (Map<String, Object>) schemaMap.getOrDefault("properties", Map.of()),
                        (List<String>) schemaMap.getOrDefault("required", List.of()),
                        null, null, null))
                    .build();
            builder.tools(
                new McpServerFeatures.AsyncToolSpecification(
                    tool,
                    null,
                    (exchange, args) -> memoryTool.handleToolCall(exchange, (java.util.Map<String, Object>) args.arguments())
                )
            );
        }

        // Register todo tools if @McpTodo annotation is present
        if (serverClass.isAnnotationPresent(McpTodo.class)) {
            TodoStore todoStoreInstance = todoStore != null ? todoStore : new InMemoryTodoStore();
            TodoTool todoTool = new TodoTool(todoStoreInstance);
            ServerMeta todoMeta = scanner.scan(TodoTool.class);
            todoMeta.getTools().forEach(t -> builder.tools(buildTool(t, todoTool, null)));
        }

        // Register planner tools if @McpPlanner annotation is present
        if (serverClass.isAnnotationPresent(McpPlanner.class)) {
            PlanStore planStoreInstance = planStore != null ? planStore : new InMemoryPlanStore();
            PlannerTool plannerTool = new PlannerTool(planStoreInstance);
            ServerMeta plannerMeta = scanner.scan(PlannerTool.class);
            plannerMeta.getTools().forEach(t -> builder.tools(buildTool(t, plannerTool, null)));
        }

        return builder.build();
    }

    private void startJetty(HttpServlet servlet) throws Exception {
        jetty = new Server(port);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        // CRITICAL: SSE requires async support to keep the connection open
        ServletHolder holder = new ServletHolder(servlet);
        holder.setAsyncSupported(true);
        ctx.addServlet(holder, mcpUri + "/*");
        jetty.setHandler(ctx);
        jetty.start();
    }

    @SuppressWarnings("unchecked")
    private McpServerFeatures.AsyncToolSpecification buildTool(ToolMeta toolMeta, Object instance, HookManager hookManager) {
        Map<String, Object> s = schemaGenerator.generate(toolMeta.getMethod());
        var tool = McpSchema.Tool.builder()
                .name(toolMeta.getName())
                .description(toolMeta.getDescription())
                .inputSchema(new McpSchema.JsonSchema(
                        (String) s.getOrDefault("type", "object"),
                        (Map<String, Object>) s.get("properties"),
                        (List<String>) s.get("required"),
                        null, null, null))
                .build();

        var handler = new ToolHandler(instance, toolMeta, new ArgumentBinder(), new ResponseMarshaller(), serverName, hookManager);
        return new McpServerFeatures.AsyncToolSpecification(tool, null, handler.asHandler());
    }

    private McpServerFeatures.AsyncResourceSpecification buildResource(ResourceMeta resourceMeta, Object instance) {
        var resource = McpSchema.Resource.builder()
                .uri(resourceMeta.getUri())
                .name(resourceMeta.getName())
                .description(resourceMeta.getDescription())
                .mimeType(resourceMeta.getMimeType())
                .build();

        var handler = new ResourceHandler(instance, resourceMeta, new ArgumentBinder(), new ResourceResponseMarshaller());
        return new McpServerFeatures.AsyncResourceSpecification(resource, handler.asHandler());
    }

    @SuppressWarnings("unchecked")
    private McpServerFeatures.AsyncPromptSpecification buildPrompt(PromptMeta promptMeta, Object instance) {
        Map<String, Object> s = schemaGenerator.generate(promptMeta.getMethod());

        // Build prompt arguments from schema
        List<McpSchema.PromptArgument> arguments = new ArrayList<>();
        if (s.containsKey("properties")) {
            Map<String, Object> properties = (Map<String, Object>) s.get("properties");
            List<String> required = (List<String>) s.getOrDefault("required", List.of());

            properties.forEach((name, propSchema) -> {
                Map<String, Object> prop = (Map<String, Object>) propSchema;
                arguments.add(new McpSchema.PromptArgument(
                        name,
                        (String) prop.getOrDefault("description", ""),
                        required.contains(name)
                ));
            });
        }

        var prompt = new McpSchema.Prompt(
                promptMeta.getName(),
                promptMeta.getDescription(),
                arguments.isEmpty() ? null : arguments
        );

        var handler = new PromptHandler(instance, promptMeta, new ArgumentBinder(), new PromptResponseMarshaller());
        return new McpServerFeatures.AsyncPromptSpecification(prompt, handler.asHandler());
    }

    private static McpTransportContext extractContext(HttpServletRequest req) {
        return McpTransportContext.create(Collections.list(req.getHeaderNames()).stream()
                .collect(Collectors.toMap(Function.identity(), req::getHeader, (a, b) -> a)));
    }

    @SuppressWarnings("unchecked")
    private Object parseJsonSchema(String schema) {
        try {
            return new ObjectMapper().readValue(schema, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}