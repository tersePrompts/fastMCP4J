package io.github.terseprompts.fastmcp.context;

import io.github.terseprompts.fastmcp.adapter.ArgumentBinder;
import io.github.terseprompts.fastmcp.adapter.ResponseMarshaller;
import io.github.terseprompts.fastmcp.adapter.ToolHandler;
import io.github.terseprompts.fastmcp.annotations.McpServer;
import io.github.terseprompts.fastmcp.annotations.McpTool;
import io.github.terseprompts.fastmcp.annotations.scanner.AnnotationScanner;
import io.github.terseprompts.fastmcp.model.ServerMeta;
import io.github.terseprompts.fastmcp.model.ToolMeta;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpLoggableSession;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LLM sampling via {@link Context#sample} (Feature 3).
 */
class SamplingTest {

    @AfterEach
    void clearContext() {
        ContextImpl.clearCurrentContext();
    }

    @Test
    void testSampleCallsWiredSampler() {
        ContextImpl context = new ContextImpl(
                new ContextImpl.RequestContext("r", "c", "s", "stdio", Map.of()),
                "TestServer",
                new ConcurrentHashMap<>(),
                noopHelper(),
                prompt -> "sampled:" + prompt,
                (prompt, maxTokens) -> "sampled:" + prompt + "@" + maxTokens
        );

        assertEquals("sampled:hello", context.sample("hello"));
        assertEquals("sampled:hello@256", context.sample("hello", 256));
    }

    @Test
    void testSampleThrowsWhenNotWired() {
        ContextImpl context = new ContextImpl(
                new ContextImpl.RequestContext("r", "c", "s", "stdio", Map.of()),
                "TestServer",
                new ConcurrentHashMap<>(),
                noopHelper()
        );

        assertThrows(UnsupportedOperationException.class, () -> context.sample("hello"));
        assertThrows(UnsupportedOperationException.class, () -> context.sample("hello", 64));
    }

    private static ContextImpl.NotificationHelper noopHelper() {
        return new ContextImpl.NotificationHelper() {
            @Override public void sendLog(String level, String message) {}
            @Override public void sendProgress(double progress, double total, String message) {}
            @Override public void sendResourceChange(String action, String uri) {}
        };
    }

    @Test
    void testContextInterfaceDefaultsThrow() {
        Context context = new Context() {
            @Override public void debug(String message) {}
            @Override public void info(String message) {}
            @Override public void warning(String message) {}
            @Override public void error(String message) {}
            @Override public void reportProgress(double progress, double total) {}
            @Override public void reportProgress(double progress, double total, String message) {}
            @Override public java.util.List<ResourceInfo> listResources() { return java.util.List.of(); }
            @Override public String readResource(String uri) { return ""; }
            @Override public java.util.List<PromptInfo> listPrompts() { return java.util.List.of(); }
            @Override public PromptResult getPrompt(String name, Map<String, Object> arguments) { return null; }
            @Override public void setState(String key, Object value) {}
            @Override public Object getState(String key) { return null; }
            @Override public void deleteState(String key) {}
            @Override public String getRequestId() { return "r"; }
            @Override public String getClientId() { return "c"; }
            @Override public String getSessionId() { return "s"; }
            @Override public String getTransport() { return "stdio"; }
            @Override public String getServerName() { return "s"; }
            @Override public Map<String, String> getHeaders() { return Map.of(); }
        };

        assertThrows(UnsupportedOperationException.class, () -> context.sample("hello"));
    }

    // ============================================
    // End-to-end wiring through ToolHandler
    // ============================================

    @McpServer(name = "SamplingServer", version = "1.0.0")
    public static class SamplingServer {
        @McpTool(description = "Sample tool")
        public String sampleTool(@McpContext Context ctx) {
            return "model said: " + ctx.sample("Summarize this");
        }
    }

    @Test
    void testToolHandlerWiresSamplerToExchange() throws Exception {
        SamplingServer server = new SamplingServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(SamplingServer.class);
        ToolMeta toolMeta = meta.getTools().get(0);

        ToolHandler handler = new ToolHandler(server, toolMeta, new ArgumentBinder(),
                new ResponseMarshaller(), "sample-server", null);

        // Fake exchange whose session answers sampling/createMessage requests.
        McpSchema.ClientCapabilities caps = McpSchema.ClientCapabilities.builder().sampling().build();
        McpAsyncServerExchange exchange = new McpAsyncServerExchange(
                "session-1", new SamplingSession(), caps,
                new McpSchema.Implementation("client", "1.0.0"),
                McpTransportContext.create(Map.of()));

        McpSchema.CallToolResult result = handler.asHandler()
                .apply(exchange, new McpSchema.CallToolRequest("sampleTool", Map.of(), Map.of()))
                .block();

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals("model said: ROFL:Summarize this",
                ((McpSchema.TextContent) result.content().get(0)).text());
    }

    static class SamplingSession implements McpLoggableSession {
        @Override
        public void setMinLoggingLevel(McpSchema.LoggingLevel level) {
        }

        @Override
        public boolean isNotificationForLevelAllowed(McpSchema.LoggingLevel level) {
            return true;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> Mono<T> sendRequest(String method, Object params, TypeRef<T> typeRef) {
            if ("sampling/createMessage".equals(method)) {
                McpSchema.CreateMessageRequest request = (McpSchema.CreateMessageRequest) params;
                String prompt = ((McpSchema.TextContent) request.messages().get(0).content()).text();
                McpSchema.CreateMessageResult result = McpSchema.CreateMessageResult.builder(
                        McpSchema.Role.ASSISTANT, "ROFL:" + prompt, "test-model").build();
                return Mono.just((T) result);
            }
            return Mono.empty();
        }

        @Override
        public Mono<Void> sendNotification(String method, Object params) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.empty();
        }

        @Override
        public void close() {
        }
    }
}