package io.github.terseprompts.fastmcp.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terseprompts.fastmcp.adapter.*;
import io.github.terseprompts.fastmcp.json.ObjectMapperFactory;
import io.github.terseprompts.fastmcp.annotations.McpMemory;
import io.github.terseprompts.fastmcp.annotations.McpTodo;
import io.github.terseprompts.fastmcp.annotations.McpPlanner;
import io.github.terseprompts.fastmcp.annotations.McpBash;
import io.github.terseprompts.fastmcp.annotations.McpFileRead;
import io.github.terseprompts.fastmcp.annotations.McpFileWrite;
import io.github.terseprompts.fastmcp.annotations.McpTelemetry;
import io.github.terseprompts.fastmcp.hook.HookManager;
import io.github.terseprompts.fastmcp.telemetry.TelemetryService;
import io.github.terseprompts.fastmcp.mcptools.bash.BashTool;
import io.github.terseprompts.fastmcp.mcptools.fileread.FileReadTool;
import io.github.terseprompts.fastmcp.mcptools.filewrite.FileWriteTool;
import io.github.terseprompts.fastmcp.mcptools.memory.InMemoryMemoryStore;
import io.github.terseprompts.fastmcp.mcptools.memory.MemoryStore;
import io.github.terseprompts.fastmcp.mcptools.memory.MemoryTool;
import io.github.terseprompts.fastmcp.mcptools.planner.InMemoryPlanStore;
import io.github.terseprompts.fastmcp.mcptools.planner.PlanStore;
import io.github.terseprompts.fastmcp.mcptools.planner.PlannerTool;
import io.github.terseprompts.fastmcp.mcptools.todo.InMemoryTodoStore;
import io.github.terseprompts.fastmcp.mcptools.todo.TodoStore;
import io.github.terseprompts.fastmcp.mcptools.todo.TodoTool;
import io.github.terseprompts.fastmcp.model.*;
import io.github.terseprompts.fastmcp.annotations.scanner.AnnotationScanner;
import io.github.terseprompts.fastmcp.adapter.schema.SchemaGenerator;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee10.servlet.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FastMCP - Fluent Builder for MCP Servers
 *
 * Supports all MCP SDK transport types:
 * - STDIO transport (for CLI tools)
 * - HTTP SSE transport (Server-Sent Events)
 * - HTTP Streamable transport (bidirectional streaming)
 *
 * Usage:
 * <pre>
 * FastMCP.server(MyServer.class)
 *     .streamable()
 *     .port(3000)
 *     .requestTimeout(Duration.ofMinutes(5))
 *     .keepAliveInterval(Duration.ofSeconds(30))
 *     .capabilities(capabilities -> capabilities
 *         .tools(true)
 *         .resources(true, true))
 *     .run();
 * </pre>
 */
public final class FastMCP {
    private final Class<?> serverClass;
    private final AnnotationScanner scanner = new AnnotationScanner();
    private final SchemaGenerator schemaGenerator = new SchemaGenerator();
    private String serverName;

    /** ThreadLocal to store transport context (headers) for current request */
    private static final ThreadLocal<Map<String, String>> TRANSPORT_CONTEXT = new ThreadLocal<>();

    // Transport configuration
    private TransportType transport = TransportType.STDIO;
    private int port = 8080;
    private String mcpUri = "/mcp";

    // Timeout and keep-alive
    private Duration requestTimeout = Duration.ofSeconds(120);
    private Duration keepAliveInterval = Duration.ofSeconds(30);

    // Server
    private Server jetty;
    private HttpServletSseServerTransportProvider sseProvider;
    private HttpServletStreamableServerTransportProvider streamableProvider;

    // Custom stores
    private MemoryStore memoryStore;
    private TodoStore todoStore;
    private PlanStore planStore;

    // Telemetry service (created from @McpTelemetry annotation)
    private TelemetryService telemetry;

    // Server capabilities configuration
    private Consumer<ServerCapabilitiesBuilder> capabilitiesConfigurer = caps -> {
        caps.tools(true)
           .resources(true, false)
           .prompts(true)
           .logging()
           .completions();
    };

    // Instructions
    private String instructions;

    private FastMCP(Class<?> serverClass) {
        this.serverClass = serverClass;
    }

    public static FastMCP server(Class<?> clazz) {
        return new FastMCP(clazz);
    }

    // ========================================
    // Transport Selection
    // ========================================

    /** STDIO transport (default, for CLI tools and local agents) */
    public FastMCP stdio() {
        this.transport = TransportType.STDIO;
        return this;
    }

    /** HTTP SSE transport (Server-Sent Events, long-lived connections) */
    public FastMCP sse() {
        this.transport = TransportType.HTTP_SSE;
        return this;
    }

    /** HTTP Streamable transport (bidirectional streaming, latest protocol) */
    public FastMCP streamable() {
        this.transport = TransportType.HTTP_STREAMABLE;
        return this;
    }

    // ========================================
    // Network Configuration
    // ========================================

    /** Set HTTP port (for SSE/Streamable transports, default: 8080) */
    public FastMCP port(int p) {
        this.port = p;
        return this;
    }

    /** Set MCP endpoint URI (default: /mcp) */
    public FastMCP mcpUri(String u) {
        this.mcpUri = u.startsWith("/") ? u : "/" + u;
        return this;
    }

    /** Set base URL for SSE transport (for constructing endpoint URLs) */
    private String baseUrl = "";

    public FastMCP baseUrl(String url) {
        this.baseUrl = url;
        return this;
    }

    // ========================================
    // Timeout Configuration
    // ========================================

    /** Set request timeout (default: 120 seconds) */
    public FastMCP requestTimeout(Duration timeout) {
        this.requestTimeout = timeout;
        return this;
    }

    /** Set request timeout in seconds */
    public FastMCP requestTimeoutSeconds(long seconds) {
        return requestTimeout(Duration.ofSeconds(seconds));
    }

    /** Set request timeout in minutes */
    public FastMCP requestTimeoutMinutes(long minutes) {
        return requestTimeout(Duration.ofMinutes(minutes));
    }

    // ========================================
    // Keep-Alive Configuration
    // ========================================

    /** Set keep-alive interval for SSE/Streamable transports (default: 30 seconds) */
    public FastMCP keepAliveInterval(Duration interval) {
        this.keepAliveInterval = interval;
        return this;
    }

    /** Set keep-alive interval in seconds */
    public FastMCP keepAliveSeconds(long seconds) {
        return keepAliveInterval(Duration.ofSeconds(seconds));
    }

    /** Disable keep-alive */
    public FastMCP disableKeepAlive() {
        this.keepAliveInterval = null;
        return this;
    }

    // ========================================
    // Server Capabilities Configuration
    // ========================================

    /**
     * Configure server capabilities using a fluent builder.
     *
     * Usage:
     * <pre>
     * .capabilities(capabilities -> capabilities
     *     .tools(true)                              // enable tools
     *     .resources(true, true)                    // enable resources with subscribe
     *     .prompts(true)                            // enable prompts
     *     .logging()                                // enable logging
     *     .completions()                            // enable completions
     * )
     * </pre>
     */
    public FastMCP capabilities(Consumer<ServerCapabilitiesBuilder> configurer) {
        this.capabilitiesConfigurer = configurer;
        return this;
    }

    /** Enable tools capability */
    public FastMCP tools(boolean listChanged) {
        return capabilities(c -> c.tools(listChanged));
    }

    /** Enable resources capability */
    public FastMCP resources(boolean subscribe, boolean listChanged) {
        return capabilities(c -> c.resources(subscribe, listChanged));
    }

    /** Enable prompts capability */
    public FastMCP prompts(boolean listChanged) {
        return capabilities(c -> c.prompts(listChanged));
    }

    /** Enable logging capability */
    public FastMCP logging() {
        return capabilities(c -> c.logging());
    }

    /** Enable completions capability */
    public FastMCP completions() {
        return capabilities(c -> c.completions());
    }

    // ========================================
    // Custom Stores
    // ========================================

    /** Set a custom MemoryStore implementation (default: InMemoryMemoryStore) */
    public FastMCP memoryStore(MemoryStore store) {
        this.memoryStore = store;
        return this;
    }

    /** Set a custom TodoStore implementation (default: InMemoryTodoStore) */
    public FastMCP todoStore(TodoStore store) {
        this.todoStore = store;
        return this;
    }

    /** Set a custom PlanStore implementation (default: InMemoryPlanStore) */
    public FastMCP planStore(PlanStore store) {
        this.planStore = store;
        return this;
    }

    // ========================================
    // Instructions
    // ========================================

    /** Set server instructions (overrides @McpServer instructions) */
    public FastMCP instructions(String instructions) {
        this.instructions = instructions;
        return this;
    }

    // ========================================
    // Lifecycle
    // ========================================

    /** Build and run the MCP server (blocking) */
    public void run() {
        try {
            McpAsyncServer mcp = build();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                mcp.closeGracefully().block(Duration.ofSeconds(10));
                if (telemetry != null) telemetry.close();
                if (jetty != null) try { jetty.stop(); } catch (Exception ignored) {}
            }));

            if (jetty != null) {
                jetty.join();
            } else {
                Thread.currentThread().join();
            }
        } catch (Exception e) {
            throw new RuntimeException("FastMCP Startup Failed", e);
        }
    }

    /** Build the MCP server without running (for advanced use cases) */
    public McpAsyncServer build() {
        try {
            Object instance = serverClass.getDeclaredConstructor().newInstance();
            ServerMeta meta = scanner.scan(serverClass);
            serverName = meta.getName();

            // Create telemetry service if @McpTelemetry annotation is present
            McpTelemetry telemetryAnn = serverClass.getAnnotation(McpTelemetry.class);
            if (telemetryAnn != null && telemetryAnn.enabled()) {
                this.telemetry = TelemetryService.create(serverName, telemetryAnn);
            }

            HookManager hookManager = new HookManager(instance, meta.getTools());
            var mapper = new JacksonMcpJsonMapper(ObjectMapperFactory.createNew());

            Object builder = createBuilder(mapper);
            configureServerInfo(builder, meta);
            configureCapabilities(builder);

            registerTools(builder, meta, instance, hookManager);
            registerResources(builder, meta, instance);
            registerPrompts(builder, meta, instance);
            registerBuiltinTools(builder);

            McpAsyncServer server = (McpAsyncServer) builder.getClass().getMethod("build").invoke(builder);
            // Start Jetty after session factory is set
            if (sseProvider != null) startJetty(sseProvider);
            else if (streamableProvider != null) startJetty(streamableProvider);
            return server;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build MCP server", e);
        }
    }

    // ========================================
    // Internal Builder Methods
    // ========================================

    private Object createBuilder(JacksonMcpJsonMapper mapper) throws Exception {
        return switch (transport) {
            case STDIO -> io.modelcontextprotocol.server.McpServer.async(new StdioServerTransportProvider(mapper));

            case HTTP_SSE -> {
                HttpServletSseServerTransportProvider.Builder builder = HttpServletSseServerTransportProvider.builder()
                        .jsonMapper(mapper)
                        .messageEndpoint(mcpUri)
                        .contextExtractor(FastMCP::extractContext);

                if (!baseUrl.isEmpty()) {
                    builder.baseUrl(baseUrl);
                }
                if (keepAliveInterval != null) {
                    builder.keepAliveInterval(keepAliveInterval);
                }

                sseProvider = builder.build();
                yield io.modelcontextprotocol.server.McpServer.async(sseProvider);
            }

            case HTTP_STREAMABLE -> {
                HttpServletStreamableServerTransportProvider.Builder builder = HttpServletStreamableServerTransportProvider.builder()
                        .jsonMapper(mapper)
                        .mcpEndpoint(mcpUri)
                        .contextExtractor(FastMCP::extractContext);

                if (keepAliveInterval != null) {
                    builder.keepAliveInterval(keepAliveInterval);
                }

                streamableProvider = builder.build();
                yield io.modelcontextprotocol.server.McpServer.async(streamableProvider);
            }
        };
    }

    private void configureServerInfo(Object builder, ServerMeta meta) {
        try {
            var method = builder.getClass().getMethod("serverInfo", String.class, String.class);
            method.invoke(builder, meta.getName(), meta.getVersion());
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure server info", e);
        }
    }

    private void configureCapabilities(Object builder) {
        ServerCapabilitiesBuilder capsBuilder = new ServerCapabilitiesBuilder();
        capabilitiesConfigurer.accept(capsBuilder);

        try {
            var capabilitiesMethod = builder.getClass().getMethod("capabilities", McpSchema.ServerCapabilities.class);
            capabilitiesMethod.invoke(builder, capsBuilder.build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure capabilities", e);
        }
    }

    private void registerTools(Object builder, ServerMeta meta, Object instance, HookManager hookManager) {
        ServerCapabilitiesBuilder caps = new ServerCapabilitiesBuilder();
        capabilitiesConfigurer.accept(caps);
        if (!caps.hasTools()) return;

        try {
            var toolsMethod = builder.getClass().getMethod("tools", McpServerFeatures.AsyncToolSpecification[].class);
            List<McpServerFeatures.AsyncToolSpecification> specs = new ArrayList<>();
            for (var tool : meta.getTools()) {
                specs.add(buildTool(tool, instance, hookManager));
            }
            toolsMethod.invoke(builder, (Object) specs.toArray(McpServerFeatures.AsyncToolSpecification[]::new));
        } catch (Exception e) {
            throw new RuntimeException("Failed to register tools", e);
        }
    }

    private void registerResources(Object builder, ServerMeta meta, Object instance) {
        ServerCapabilitiesBuilder caps = new ServerCapabilitiesBuilder();
        capabilitiesConfigurer.accept(caps);
        if (!caps.hasResources()) return;

        try {
            var resourcesMethod = builder.getClass().getMethod("resources", List.class);
            List<McpServerFeatures.AsyncResourceSpecification> specs = new ArrayList<>();
            for (var resource : meta.getResources()) {
                specs.add(buildResource(resource, instance));
            }
            resourcesMethod.invoke(builder, specs);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register resources", e);
        }
    }

    private void registerPrompts(Object builder, ServerMeta meta, Object instance) {
        ServerCapabilitiesBuilder caps = new ServerCapabilitiesBuilder();
        capabilitiesConfigurer.accept(caps);
        if (!caps.hasPrompts()) return;

        try {
            var promptsMethod = builder.getClass().getMethod("prompts", List.class);
            List<McpServerFeatures.AsyncPromptSpecification> specs = new ArrayList<>();
            for (var prompt : meta.getPrompts()) {
                specs.add(buildPrompt(prompt, instance));
            }
            promptsMethod.invoke(builder, specs);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register prompts", e);
        }
    }

    private void registerBuiltinTools(Object builder) {
        try {
            List<McpServerFeatures.AsyncToolSpecification> specs = new ArrayList<>();
            var toolsMethod = builder.getClass().getMethod("tools", McpServerFeatures.AsyncToolSpecification[].class);

            if (serverClass.isAnnotationPresent(McpMemory.class)) {
                MemoryStore store = memoryStore != null ? memoryStore : new InMemoryMemoryStore();
                MemoryTool memoryTool = new MemoryTool(store);
                specs.add(buildBuiltinTool(memoryTool, "memory",
                    "Memory tool for persistent storage and retrieval of information"));
            }

            if (serverClass.isAnnotationPresent(McpTodo.class)) {
                TodoStore store = todoStore != null ? todoStore : new InMemoryTodoStore();
                TodoTool todoTool = new TodoTool(store);
                for (var t : scanner.scanToolsOnly(TodoTool.class)) {
                    specs.add(buildTool(t, todoTool, null));
                }
            }

            if (serverClass.isAnnotationPresent(McpPlanner.class)) {
                PlanStore store = planStore != null ? planStore : new InMemoryPlanStore();
                PlannerTool plannerTool = new PlannerTool(store);
                for (var t : scanner.scanToolsOnly(PlannerTool.class)) {
                    specs.add(buildTool(t, plannerTool, null));
                }
            }

            if (serverClass.isAnnotationPresent(McpFileRead.class)) {
                FileReadTool fileReadTool = new FileReadTool();
                for (var t : scanner.scanToolsOnly(FileReadTool.class)) {
                    specs.add(buildTool(t, fileReadTool, null));
                }
            }

            if (serverClass.isAnnotationPresent(McpFileWrite.class)) {
                FileWriteTool fileWriteTool = new FileWriteTool();
                for (var t : scanner.scanToolsOnly(FileWriteTool.class)) {
                    specs.add(buildTool(t, fileWriteTool, null));
                }
            }

            if (serverClass.isAnnotationPresent(McpBash.class)) {
                McpBash bashAnn = serverClass.getAnnotation(McpBash.class);
                BashTool bashTool = new BashTool(
                    bashAnn.timeout(),
                    bashAnn.visibleAfterBasePath(),
                    List.of(bashAnn.notAllowedPaths())
                );
                specs.add(buildBuiltinTool(bashTool, "bash", bashTool.getToolDescription()));
            }

            if (!specs.isEmpty()) {
                toolsMethod.invoke(builder, (Object) specs.toArray(McpServerFeatures.AsyncToolSpecification[]::new));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to register builtin tools", e);
        }
    }

    private void startJetty(HttpServlet servlet) throws Exception {
        jetty = new Server(port);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ServletHolder holder = new ServletHolder(servlet);
        holder.setAsyncSupported(true);
        ctx.addServlet(holder, mcpUri + "/*");

        // Add security headers filter
        FilterHolder securityFilter = new FilterHolder(new SecurityHeadersFilter());
        securityFilter.setAsyncSupported(true);
        ctx.addFilter(securityFilter, "/*", java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST));

        jetty.setHandler(ctx);
        jetty.start();
    }

    private void startJetty(HttpServletSseServerTransportProvider provider) throws Exception {
        jetty = new Server(port);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ServletHolder holder = new ServletHolder(provider);
        holder.setAsyncSupported(true);
        // SSE requires two endpoints: /sse for GET (SSE connection) and /mcp for POST (messages)
        ctx.addServlet(holder, "/sse/*");
        ctx.addServlet(holder, mcpUri + "/*");

        // Add security headers filter
        FilterHolder securityFilter = new FilterHolder(new SecurityHeadersFilter());
        securityFilter.setAsyncSupported(true);
        ctx.addFilter(securityFilter, "/*", java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST));

        jetty.setHandler(ctx);
        jetty.start();
    }

    private void startJetty(HttpServletStreamableServerTransportProvider provider) throws Exception {
        startJetty((HttpServlet) provider);
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

        var handler = new ToolHandler(instance, toolMeta, new ArgumentBinder(),
            new ResponseMarshaller(), serverName, hookManager, telemetry);
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

    @SuppressWarnings("unchecked")
    private McpServerFeatures.AsyncToolSpecification buildBuiltinTool(
            Object toolInstance, String name, String description) {
        try {
            var method = toolInstance.getClass().getMethod("getToolSchema");
            String toolSchema = (String) method.invoke(toolInstance);
            Map<String, Object> schemaMap = (Map<String, Object>) parseJsonSchema(toolSchema);

            var tool = McpSchema.Tool.builder()
                    .name(name)
                    .description(description)
                    .inputSchema(new McpSchema.JsonSchema(
                        (String) schemaMap.getOrDefault("type", "object"),
                        (Map<String, Object>) schemaMap.getOrDefault("properties", Map.of()),
                        (List<String>) schemaMap.getOrDefault("required", List.of()),
                        null, null, null))
                    .build();

            return new McpServerFeatures.AsyncToolSpecification(
                tool,
                null,
                (McpAsyncServerExchange exchange, McpSchema.CallToolRequest args) -> {
                    try {
                        var handleMethod = toolInstance.getClass().getMethod(
                            "handleToolCall", McpAsyncServerExchange.class, Map.class);
                        return (Mono<McpSchema.CallToolResult>) handleMethod.invoke(
                            toolInstance, exchange, args.arguments());
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                }
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to build builtin tool: " + name, e);
        }
    }

    private static McpTransportContext extractContext(HttpServletRequest req) {
        Map<String, String> headers = Collections.list(req.getHeaderNames()).stream()
                .collect(Collectors.toMap(Function.identity(), req::getHeader, (a, b) -> a));
        TRANSPORT_CONTEXT.set(headers);
        @SuppressWarnings("unchecked")
        Map<String, Object> headersAsObject = (Map<String, Object>) (Map<?, ?>) headers;
        return McpTransportContext.create(headersAsObject);
    }

    /** Get the current transport context (headers) from ThreadLocal */
    public static Map<String, String> getTransportContext() {
        return TRANSPORT_CONTEXT.get();
    }

    /** Clear the current transport context */
    public static void clearTransportContext() {
        TRANSPORT_CONTEXT.remove();
    }

    /**
     * Get the telemetry service for the current server (if enabled).
     * Can be used for manual instrumentation.
     */
    public TelemetryService getTelemetry() {
        return telemetry;
    }

    @SuppressWarnings("unchecked")
    private Object parseJsonSchema(String schema) {
        try {
            return ObjectMapperFactory.getShared().readValue(schema, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    // ========================================
    // Security Headers Filter
    // ========================================

    /**
     * Filter that adds security headers to all HTTP responses.
     */
    private static class SecurityHeadersFilter implements Filter {
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            // No initialization needed
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            if (response instanceof HttpServletResponse httpResponse) {
                // Prevent MIME type sniffing
                httpResponse.setHeader("X-Content-Type-Options", "nosniff");
                // Prevent clickjacking
                httpResponse.setHeader("X-Frame-Options", "DENY");
                // Enable browser XSS filtering
                httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
                // Restrict referrer information
                httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                // Content Security Policy (basic)
                httpResponse.setHeader("Content-Security-Policy", "default-src 'self'");
            }
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
            // No cleanup needed
        }
    }

    // ========================================
    // Transport Types
    // ========================================

    private enum TransportType {
        STDIO,
        HTTP_SSE,
        HTTP_STREAMABLE
    }

    // ========================================
    // Server Capabilities Builder
    // ========================================

    /**
     * Fluent builder for MCP ServerCapabilities.
     */
    public static class ServerCapabilitiesBuilder {
        private boolean toolsEnabled = true;
        private boolean toolsListChanged = false;
        private boolean resourcesEnabled = false;
        private boolean resourcesSubscribe = false;
        private boolean resourcesListChanged = false;
        private boolean promptsEnabled = false;
        private boolean promptsListChanged = false;
        private boolean loggingEnabled = false;
        private boolean completionsEnabled = false;

        public ServerCapabilitiesBuilder tools(boolean listChanged) {
            this.toolsEnabled = true;
            this.toolsListChanged = listChanged;
            return this;
        }

        public ServerCapabilitiesBuilder noTools() {
            this.toolsEnabled = false;
            return this;
        }

        public ServerCapabilitiesBuilder resources(boolean subscribe, boolean listChanged) {
            this.resourcesEnabled = true;
            this.resourcesSubscribe = subscribe;
            this.resourcesListChanged = listChanged;
            return this;
        }

        public ServerCapabilitiesBuilder noResources() {
            this.resourcesEnabled = false;
            return this;
        }

        public ServerCapabilitiesBuilder prompts(boolean listChanged) {
            this.promptsEnabled = true;
            this.promptsListChanged = listChanged;
            return this;
        }

        public ServerCapabilitiesBuilder noPrompts() {
            this.promptsEnabled = false;
            return this;
        }

        public ServerCapabilitiesBuilder logging() {
            this.loggingEnabled = true;
            return this;
        }

        public ServerCapabilitiesBuilder completions() {
            this.completionsEnabled = true;
            return this;
        }

        public boolean hasTools() {
            return toolsEnabled;
        }

        public boolean hasResources() {
            return resourcesEnabled;
        }

        public boolean hasPrompts() {
            return promptsEnabled;
        }

        public McpSchema.ServerCapabilities build() {
            McpSchema.ServerCapabilities.Builder builder = McpSchema.ServerCapabilities.builder();

            if (toolsEnabled) {
                builder.tools(toolsListChanged);
            }

            if (resourcesEnabled) {
                builder.resources(resourcesSubscribe, resourcesListChanged);
            }

            if (promptsEnabled) {
                builder.prompts(promptsListChanged);
            }

            if (loggingEnabled) {
                builder.logging();
            }

            if (completionsEnabled) {
                builder.completions();
            }

            return builder.build();
        }
    }
}
