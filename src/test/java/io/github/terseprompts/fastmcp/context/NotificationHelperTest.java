package io.github.terseprompts.fastmcp.context;

import io.github.terseprompts.fastmcp.adapter.ProtocolNotifications;
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that progress and logging notifications are sent to the MCP client
 * (Feature 1) rather than merely printed to stderr.
 */
class NotificationHelperTest {

    private final PrintStream originalErr = System.err;

    @AfterEach
    void restoreStream() {
        System.setErr(originalErr);
        ContextImpl.clearCurrentContext();
    }

    @McpServer(name = "NotificationServer", version = "1.0.0")
    public static class NotificationServer {
        @McpTool(description = "Logging tool")
        public String logTool(@McpContext Context ctx) {
            ctx.info("hello from logTool");
            return "ok";
        }

        @McpTool(description = "Progress tool")
        public String progressTool(@McpContext Context ctx) {
            ctx.reportProgress(3, 10, "working");
            return "ok";
        }

        @McpTool(description = "Tool without token")
        public String noTokenTool(@McpContext Context ctx) {
            ctx.reportProgress(1, 2, "no token");
            ctx.debug("no token debug");
            return "ok";
        }
    }

    // ============================================
    // Pure function tests
    // ============================================

    @Test
    void testMapLevel() {
        assertEquals(McpSchema.LoggingLevel.DEBUG, ProtocolNotifications.mapLevel("debug"));
        assertEquals(McpSchema.LoggingLevel.INFO, ProtocolNotifications.mapLevel("info"));
        assertEquals(McpSchema.LoggingLevel.WARNING, ProtocolNotifications.mapLevel("warning"));
        assertEquals(McpSchema.LoggingLevel.ERROR, ProtocolNotifications.mapLevel("error"));
        assertEquals(McpSchema.LoggingLevel.NOTICE, ProtocolNotifications.mapLevel("notice"));
        assertEquals(McpSchema.LoggingLevel.CRITICAL, ProtocolNotifications.mapLevel("critical"));
        assertEquals(McpSchema.LoggingLevel.ALERT, ProtocolNotifications.mapLevel("alert"));
        assertEquals(McpSchema.LoggingLevel.EMERGENCY, ProtocolNotifications.mapLevel("emergency"));
        // Case-insensitive and safe defaults
        assertEquals(McpSchema.LoggingLevel.DEBUG, ProtocolNotifications.mapLevel("DEBUG"));
        assertEquals(McpSchema.LoggingLevel.INFO, ProtocolNotifications.mapLevel("unknown-level"));
        assertEquals(McpSchema.LoggingLevel.INFO, ProtocolNotifications.mapLevel(null));
    }

    @Test
    void testBuildLogNotification() {
        McpSchema.LoggingMessageNotification n =
                ProtocolNotifications.buildLogNotification("warning", "dent detected");

        assertEquals(McpSchema.LoggingLevel.WARNING, n.level());
        assertEquals("fastmcp", n.logger());
        assertEquals("dent detected", n.data());
    }

    @Test
    void testBuildProgressNotification() {
        McpSchema.ProgressNotification n = ProtocolNotifications.buildProgressNotification(
                "tok-1", 5.0, 10.0, "halfway");

        assertEquals("tok-1", n.progressToken());
        assertEquals(5.0, n.progress());
        assertEquals(10.0, n.total());
        assertEquals("halfway", n.message());

        McpSchema.ProgressNotification minimal =
                ProtocolNotifications.buildProgressNotification("tok-2", 1.0, null, null);
        assertEquals("tok-2", minimal.progressToken());
        assertNull(minimal.total());
        assertNull(minimal.message());
    }

    // ============================================
    // End-to-end via a faked session/exchange
    // ============================================

    @Test
    void testToolHandlerSendsLogNotification() throws Exception {
        RecordingSession session = new RecordingSession();
        McpAsyncServerExchange exchange = fakeExchange(session);

        ToolHandler handler = handlerFor("logTool");
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "logTool", Map.of(), Map.of("progressToken", "tok-log"));
        McpSchema.CallToolResult result = handler.asHandler().apply(exchange, request).block();

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals("ok", ((McpSchema.TextContent) result.content().get(0)).text());

        assertEquals(List.of("notifications/message"), session.notificationMethods);
        McpSchema.LoggingMessageNotification sent =
                (McpSchema.LoggingMessageNotification) session.notificationParams.get(0);
        assertEquals(McpSchema.LoggingLevel.INFO, sent.level());
        assertEquals("fastmcp", sent.logger());
        assertEquals("hello from logTool", sent.data());
    }

    @Test
    void testToolHandlerSendsProgressNotification() throws Exception {
        RecordingSession session = new RecordingSession();
        McpAsyncServerExchange exchange = fakeExchange(session);

        ToolHandler handler = handlerFor("progressTool");
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "progressTool", Map.of(), Map.of("progressToken", "tok-progress"));
        McpSchema.CallToolResult result = handler.asHandler().apply(exchange, request).block();

        assertNotNull(result);
        assertFalse(result.isError());

        assertEquals(List.of("notifications/progress"), session.notificationMethods);
        McpSchema.ProgressNotification sent =
                (McpSchema.ProgressNotification) session.notificationParams.get(0);
        assertEquals("tok-progress", sent.progressToken());
        assertEquals(3.0, sent.progress());
        assertEquals(10.0, sent.total());
        assertEquals("working", sent.message());
    }

    @Test
    void testToolHandlerDoesNotPrintToStderrWhenNotificationSucceeds() throws Exception {
        RecordingSession session = new RecordingSession();
        McpAsyncServerExchange exchange = fakeExchange(session);

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(stderr));

        ToolHandler handler = handlerFor("logTool");
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                "logTool", Map.of(), Map.of("progressToken", "tok"));
        McpSchema.CallToolResult result = handler.asHandler().apply(exchange, request).block();

        assertNotNull(result);
        assertFalse(result.isError());
        // The old stub printed "[info] hello from logTool"; slf4j may still log to stderr.
        assertFalse(stderr.toString().contains("[info] hello from logTool"),
            "A successful notification should no longer be printed to stderr");
    }

    @Test
    void testProgressWithoutTokenIsSkippedNotSent() throws Exception {
        RecordingSession session = new RecordingSession();
        McpAsyncServerExchange exchange = fakeExchange(session);

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(stderr));

        ToolHandler handler = handlerFor("noTokenTool");
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("noTokenTool", Map.of(), Map.of());
        McpSchema.CallToolResult result = handler.asHandler().apply(exchange, request).block();

        assertNotNull(result);
        assertFalse(result.isError());

        // Without a progress token the progress notification cannot be delivered;
        // it falls back to stderr rather than being sent. The debug() log notification
        // is still delivered (it does not need a token).
        assertEquals(List.of("notifications/message"), session.notificationMethods,
            "Only the log notification should be sent when there is no progress token");
        assertFalse(session.notificationMethods.contains("notifications/progress"));
        assertTrue(stderr.toString().contains("[Progress]"));
        assertTrue(stderr.toString().contains("no token"));
    }

    private ToolHandler handlerFor(String toolName) throws Exception {
        NotificationServer server = new NotificationServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(NotificationServer.class);
        ToolMeta toolMeta = meta.getTools().stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst()
                .orElseThrow();
        return new ToolHandler(server, toolMeta, new ArgumentBinder(),
                new ResponseMarshaller(), "notif-server", null);
    }

    private McpAsyncServerExchange fakeExchange(McpLoggableSession session) {
        McpSchema.ClientCapabilities caps = McpSchema.ClientCapabilities.builder().sampling().build();
        return new McpAsyncServerExchange(
                "session-1",
                session,
                caps,
                new McpSchema.Implementation("test-client", "1.0.0"),
                McpTransportContext.create(Map.of())
        );
    }

    /**
     * Fake session that records outbound notifications instead of transporting them.
     */
    static class RecordingSession implements McpLoggableSession {
        final List<String> notificationMethods = new ArrayList<>();
        final List<Object> notificationParams = new ArrayList<>();

        @Override
        public void setMinLoggingLevel(McpSchema.LoggingLevel level) {
        }

        @Override
        public boolean isNotificationForLevelAllowed(McpSchema.LoggingLevel level) {
            return true;
        }

        @Override
        public <T> Mono<T> sendRequest(String method, Object params, TypeRef<T> typeRef) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> sendNotification(String method, Object params) {
            notificationMethods.add(method);
            notificationParams.add(params);
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