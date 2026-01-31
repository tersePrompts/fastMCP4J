package io.github.terseprompts.fastmcp.core;

import io.github.terseprompts.fastmcp.annotations.McpServer;
import io.github.terseprompts.fastmcp.annotations.McpTool;
import io.github.terseprompts.fastmcp.mcptools.memory.InMemoryMemoryStore;
import io.github.terseprompts.fastmcp.mcptools.memory.MemoryStore;
import io.github.terseprompts.fastmcp.mcptools.todo.InMemoryTodoStore;
import io.github.terseprompts.fastmcp.mcptools.todo.TodoStore;
import io.github.terseprompts.fastmcp.mcptools.planner.InMemoryPlanStore;
import io.github.terseprompts.fastmcp.mcptools.planner.PlanStore;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FastMCP fluent builder API.
 */
class FastMCPTest {

    @McpServer(name = "TestServer", version = "1.0.0")
    public static class SimpleTestServer {
        @McpTool(description = "Echo tool")
        public String echo(String message) {
            return "echo: " + message;
        }
    }

    // ========================================
    // Transport Selection Tests
    // ========================================

    @Test
    void testDefaultTransportIsStdio() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class);
        String transport = getTransportField(mcp);
        assertEquals("STDIO", transport);
    }

    @Test
    void testStdioTransport() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).stdio();
        String transport = getTransportField(mcp);
        assertEquals("STDIO", transport);
    }

    @Test
    void testSseTransport() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).sse();
        String transport = getTransportField(mcp);
        assertEquals("HTTP_SSE", transport);
    }

    @Test
    void testStreamableTransport() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).streamable();
        String transport = getTransportField(mcp);
        assertEquals("HTTP_STREAMABLE", transport);
    }

    // ========================================
    // Network Configuration Tests
    // ========================================

    @Test
    void testDefaultPort() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class);
        int port = getPortField(mcp);
        assertEquals(8080, port);
    }

    @Test
    void testCustomPort() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).port(3000);
        int port = getPortField(mcp);
        assertEquals(3000, port);
    }

    @Test
    void testDefaultMcpUri() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class);
        String uri = getMcpUriField(mcp);
        assertEquals("/mcp", uri);
    }

    @Test
    void testCustomMcpUri() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).mcpUri("/api/mcp");
        String uri = getMcpUriField(mcp);
        assertEquals("/api/mcp", uri);
    }

    @Test
    void testMcpUriWithoutLeadingSlash() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).mcpUri("mcp");
        String uri = getMcpUriField(mcp);
        assertEquals("/mcp", uri);
    }

    @Test
    void testBaseUrl() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).baseUrl("https://example.com");
        String baseUrl = getBaseUrlField(mcp);
        assertEquals("https://example.com", baseUrl);
    }

    // ========================================
    // Timeout Configuration Tests
    // ========================================

    @Test
    void testDefaultRequestTimeout() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class);
        Duration timeout = getRequestTimeoutField(mcp);
        assertEquals(Duration.ofSeconds(120), timeout);
    }

    @Test
    void testRequestTimeout() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).requestTimeout(Duration.ofMinutes(5));
        Duration timeout = getRequestTimeoutField(mcp);
        assertEquals(Duration.ofMinutes(5), timeout);
    }

    @Test
    void testRequestTimeoutSeconds() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).requestTimeoutSeconds(60);
        Duration timeout = getRequestTimeoutField(mcp);
        assertEquals(Duration.ofSeconds(60), timeout);
    }

    @Test
    void testRequestTimeoutMinutes() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).requestTimeoutMinutes(10);
        Duration timeout = getRequestTimeoutField(mcp);
        assertEquals(Duration.ofMinutes(10), timeout);
    }

    // ========================================
    // Keep-Alive Configuration Tests
    // ========================================

    @Test
    void testDefaultKeepAliveInterval() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class);
        Duration interval = getKeepAliveIntervalField(mcp);
        assertEquals(Duration.ofSeconds(30), interval);
    }

    @Test
    void testKeepAliveInterval() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).keepAliveInterval(Duration.ofMinutes(1));
        Duration interval = getKeepAliveIntervalField(mcp);
        assertEquals(Duration.ofMinutes(1), interval);
    }

    @Test
    void testKeepAliveSeconds() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).keepAliveSeconds(45);
        Duration interval = getKeepAliveIntervalField(mcp);
        assertEquals(Duration.ofSeconds(45), interval);
    }

    @Test
    void testDisableKeepAlive() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).disableKeepAlive();
        Duration interval = getKeepAliveIntervalField(mcp);
        assertNull(interval);
    }

    // ========================================
    // Server Capabilities Tests
    // ========================================

    @Test
    void testDefaultCapabilities() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class);
        FastMCP.ServerCapabilitiesBuilder builder = getCapabilitiesBuilder(mcp);
        McpSchema.ServerCapabilities caps = builder.build();

        // Default capabilities from FastMCP: tools(true), resources(true, false), prompts(true), logging(), completions()
        assertNotNull(caps.tools());
        assertTrue(caps.tools().listChanged());
        assertNotNull(caps.resources());
        assertTrue(caps.resources().subscribe()); // resources(true, false) means subscribe=true
        assertFalse(caps.resources().listChanged());
        assertNotNull(caps.prompts());
        assertTrue(caps.prompts().listChanged()); // prompts(true) means listChanged=true
        assertNotNull(caps.logging());
        assertNotNull(caps.completions());
    }

    @Test
    void testCustomCapabilities() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class)
                .capabilities(c -> c
                        .tools(true)
                        .resources(true, true)
                        .noPrompts()
                        .logging()
                        .completions());
        FastMCP.ServerCapabilitiesBuilder builder = getCapabilitiesBuilder(mcp);
        McpSchema.ServerCapabilities caps = builder.build();

        assertNotNull(caps.tools());
        assertTrue(caps.tools().listChanged());
        assertNotNull(caps.resources());
        assertTrue(caps.resources().subscribe());
        assertTrue(caps.resources().listChanged());
        assertNull(caps.prompts());
        assertNotNull(caps.logging());
        assertNotNull(caps.completions());
    }

    @Test
    void testToolsCapability() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).tools(true);
        FastMCP.ServerCapabilitiesBuilder builder = getCapabilitiesBuilder(mcp);
        McpSchema.ServerCapabilities caps = builder.build();

        assertNotNull(caps.tools());
        assertTrue(caps.tools().listChanged());
    }

    @Test
    void testResourcesCapability() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).resources(true, false);
        FastMCP.ServerCapabilitiesBuilder builder = getCapabilitiesBuilder(mcp);
        McpSchema.ServerCapabilities caps = builder.build();

        assertNotNull(caps.resources());
        assertTrue(caps.resources().subscribe());
        assertFalse(caps.resources().listChanged());
    }

    @Test
    void testPromptsCapability() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).prompts(true);
        FastMCP.ServerCapabilitiesBuilder builder = getCapabilitiesBuilder(mcp);
        McpSchema.ServerCapabilities caps = builder.build();

        assertNotNull(caps.prompts());
        assertTrue(caps.prompts().listChanged());
    }

    @Test
    void testLoggingCapability() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).logging();
        FastMCP.ServerCapabilitiesBuilder builder = getCapabilitiesBuilder(mcp);
        McpSchema.ServerCapabilities caps = builder.build();

        assertNotNull(caps.logging());
    }

    @Test
    void testCompletionsCapability() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).completions();
        FastMCP.ServerCapabilitiesBuilder builder = getCapabilitiesBuilder(mcp);
        McpSchema.ServerCapabilities caps = builder.build();

        assertNotNull(caps.completions());
    }

    // ========================================
    // Custom Stores Tests
    // ========================================

    @Test
    void testMemoryStore() throws Exception {
        MemoryStore customStore = new InMemoryMemoryStore();
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).memoryStore(customStore);
        MemoryStore store = getMemoryStoreField(mcp);
        assertSame(customStore, store);
    }

    @Test
    void testTodoStore() throws Exception {
        TodoStore customStore = new InMemoryTodoStore();
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).todoStore(customStore);
        TodoStore store = getTodoStoreField(mcp);
        assertSame(customStore, store);
    }

    @Test
    void testPlanStore() throws Exception {
        PlanStore customStore = new InMemoryPlanStore();
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).planStore(customStore);
        PlanStore store = getPlanStoreField(mcp);
        assertSame(customStore, store);
    }

    // ========================================
    // Instructions Tests
    // ========================================

    @Test
    void testInstructions() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class).instructions("Custom instructions");
        String instructions = getInstructionsField(mcp);
        assertEquals("Custom instructions", instructions);
    }

    // ========================================
    // Fluent API Chaining Tests
    // ========================================

    @Test
    void testFluentApiChaining() throws Exception {
        FastMCP mcp = FastMCP.server(SimpleTestServer.class)
                .streamable()
                .port(3000)
                .mcpUri("/api/mcp")
                .requestTimeoutMinutes(5)
                .keepAliveSeconds(60)
                .tools(true)
                .instructions("Test instructions");

        // Verify all values were set correctly
        assertEquals("HTTP_STREAMABLE", getTransportField(mcp));
        assertEquals(3000, getPortField(mcp));
        assertEquals("/api/mcp", getMcpUriField(mcp));
        assertEquals(Duration.ofMinutes(5), getRequestTimeoutField(mcp));
        assertEquals(Duration.ofSeconds(60), getKeepAliveIntervalField(mcp));
        assertEquals("Test instructions", getInstructionsField(mcp));
    }

    // ========================================
    // ServerCapabilitiesBuilder Tests
    // ========================================

    @Test
    void testServerCapabilitiesBuilderDefault() {
        FastMCP.ServerCapabilitiesBuilder builder = new FastMCP.ServerCapabilitiesBuilder();
        assertTrue(builder.hasTools());
        assertFalse(builder.hasResources());
        assertFalse(builder.hasPrompts());
    }

    @Test
    void testServerCapabilitiesBuilderNoTools() {
        FastMCP.ServerCapabilitiesBuilder builder = new FastMCP.ServerCapabilitiesBuilder().noTools();
        McpSchema.ServerCapabilities caps = builder.build();
        assertNull(caps.tools());
    }

    @Test
    void testServerCapabilitiesBuilderNoResources() {
        FastMCP.ServerCapabilitiesBuilder builder = new FastMCP.ServerCapabilitiesBuilder().noResources();
        McpSchema.ServerCapabilities caps = builder.build();
        assertNull(caps.resources());
    }

    @Test
    void testServerCapabilitiesBuilderNoPrompts() {
        FastMCP.ServerCapabilitiesBuilder builder = new FastMCP.ServerCapabilitiesBuilder().noPrompts();
        McpSchema.ServerCapabilities caps = builder.build();
        assertNull(caps.prompts());
    }

    @Test
    void testServerCapabilitiesBuilderAllDisabled() {
        FastMCP.ServerCapabilitiesBuilder builder = new FastMCP.ServerCapabilitiesBuilder()
                .noTools()
                .noResources()
                .noPrompts();
        McpSchema.ServerCapabilities caps = builder.build();

        assertNull(caps.tools());
        assertNull(caps.resources());
        assertNull(caps.prompts());
        assertNull(caps.logging());
        assertNull(caps.completions());
    }

    @Test
    void testServerCapabilitiesBuilderAllEnabled() {
        FastMCP.ServerCapabilitiesBuilder builder = new FastMCP.ServerCapabilitiesBuilder()
                .tools(true)
                .resources(true, true)
                .prompts(true)
                .logging()
                .completions();
        McpSchema.ServerCapabilities caps = builder.build();

        assertNotNull(caps.tools());
        assertTrue(caps.tools().listChanged());
        assertNotNull(caps.resources());
        assertTrue(caps.resources().subscribe());
        assertTrue(caps.resources().listChanged());
        assertNotNull(caps.prompts());
        assertTrue(caps.prompts().listChanged());
        assertNotNull(caps.logging());
        assertNotNull(caps.completions());
    }

    // ========================================
    // Helper Methods for Reflection
    // ========================================

    private String getTransportField(FastMCP mcp) throws Exception {
        Field field = FastMCP.class.getDeclaredField("transport");
        field.setAccessible(true);
        Object value = field.get(mcp);
        return value.toString();
    }

    private int getPortField(FastMCP mcp) throws Exception {
        Field field = FastMCP.class.getDeclaredField("port");
        field.setAccessible(true);
        return (int) field.get(mcp);
    }

    private String getMcpUriField(FastMCP mcp) throws Exception {
        Field field = FastMCP.class.getDeclaredField("mcpUri");
        field.setAccessible(true);
        return (String) field.get(mcp);
    }

    private String getBaseUrlField(FastMCP mcp) throws Exception {
        Field field = FastMCP.class.getDeclaredField("baseUrl");
        field.setAccessible(true);
        return (String) field.get(mcp);
    }

    private Duration getRequestTimeoutField(FastMCP mcp) throws Exception {
        Field field = FastMCP.class.getDeclaredField("requestTimeout");
        field.setAccessible(true);
        return (Duration) field.get(mcp);
    }

    private Duration getKeepAliveIntervalField(FastMCP mcp) throws Exception {
        Field field = FastMCP.class.getDeclaredField("keepAliveInterval");
        field.setAccessible(true);
        return (Duration) field.get(mcp);
    }

    @SuppressWarnings("unchecked")
    private FastMCP.ServerCapabilitiesBuilder getCapabilitiesBuilder(FastMCP mcp) throws Exception {
        // Get the capabilitiesConfigurer field and invoke it on a new builder
        Field field = FastMCP.class.getDeclaredField("capabilitiesConfigurer");
        field.setAccessible(true);
        java.util.function.Consumer<FastMCP.ServerCapabilitiesBuilder> configurer =
                (java.util.function.Consumer<FastMCP.ServerCapabilitiesBuilder>) field.get(mcp);

        FastMCP.ServerCapabilitiesBuilder builder = new FastMCP.ServerCapabilitiesBuilder();
        configurer.accept(builder);
        return builder;
    }

    private MemoryStore getMemoryStoreField(FastMCP mcp) throws Exception {
        Field field = FastMCP.class.getDeclaredField("memoryStore");
        field.setAccessible(true);
        return (MemoryStore) field.get(mcp);
    }

    private TodoStore getTodoStoreField(FastMCP mcp) throws Exception {
        Field field = FastMCP.class.getDeclaredField("todoStore");
        field.setAccessible(true);
        return (TodoStore) field.get(mcp);
    }

    private PlanStore getPlanStoreField(FastMCP mcp) throws Exception {
        Field field = FastMCP.class.getDeclaredField("planStore");
        field.setAccessible(true);
        return (PlanStore) field.get(mcp);
    }

    private String getInstructionsField(FastMCP mcp) throws Exception {
        Field field = FastMCP.class.getDeclaredField("instructions");
        field.setAccessible(true);
        return (String) field.get(mcp);
    }
}
