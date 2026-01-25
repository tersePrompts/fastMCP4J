package io.github.fastmcp.handler;

import io.github.fastmcp.annotations.*;
import io.github.fastmcp.model.*;
import io.github.fastmcp.notification.NotificationSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ListHandlersTest {
    
    @McpServer(name = "TestServer")
    public static class TestServer {
        @McpTool(description = "Tool 1")
        public String tool1() { return "result1"; }
        
        @McpTool(description = "Tool 2")
        public String tool2() { return "result2"; }
        
        @McpTool(description = "Tool 3")
        public String tool3() { return "result3"; }
        
        @McpResource(uri = "file://res1.txt", description = "Resource 1")
        public String resource1() { return "res1"; }
        
        @McpResource(uri = "file://res2.txt", description = "Resource 2")
        public String resource2() { return "res2"; }
        
        @McpPrompt(description = "Prompt 1")
        public String prompt1() { return "p1"; }
        
        @McpPrompt(description = "Prompt 2")
        public String prompt2() { return "p2"; }
    }
    
    private ServerMeta meta;
    private NotificationSender notificationSender;
    private List<String> notifications;
    
    @BeforeEach
    void setUp() {
        io.github.fastmcp.scanner.AnnotationScanner scanner = new io.github.fastmcp.scanner.AnnotationScanner();
        meta = scanner.scan(TestServer.class);
        notifications = new ArrayList<>();
        notificationSender = new NotificationSender(message -> notifications.add(message.toString()));
    }
    
    @Test
    void testListToolsFirstPage() {
        ListToolsHandler handler = new ListToolsHandler(meta, notificationSender, 2);
        
        Map<String, Object> request = new HashMap<>();
        request.put("cursor", null);
        
        Mono<ListToolsResult> resultMono = handler.asHandler().apply(null, request);
        ListToolsResult result = resultMono.block();
        
        assertNotNull(result);
        assertEquals(2, result.tools().size());
        assertNotNull(result.nextCursor());
        assertTrue(result.nextCursor().length() > 0);
    }
    
    @Test
    void testListToolsSecondPage() {
        ListToolsHandler handler = new ListToolsHandler(meta, notificationSender, 2);
        
        Map<String, Object> firstRequest = new HashMap<>();
        firstRequest.put("cursor", null);
        
        Mono<ListToolsResult> firstResultMono = handler.asHandler().apply(null, firstRequest);
        ListToolsResult firstResult = firstResultMono.block();
        
        Map<String, Object> secondRequest = new HashMap<>();
        secondRequest.put("cursor", firstResult.nextCursor());
        
        Mono<ListToolsResult> secondResultMono = handler.asHandler().apply(null, secondRequest);
        ListToolsResult secondResult = secondResultMono.block();
        
        assertNotNull(secondResult);
        assertEquals(1, secondResult.tools().size());
        assertNull(secondResult.nextCursor());
    }
    
    @Test
    void testListResourcesPagination() {
        ListResourcesHandler handler = new ListResourcesHandler(meta, notificationSender, 1);
        
        Map<String, Object> request = new HashMap<>();
        request.put("cursor", null);
        
        Mono<ListResourcesResult> resultMono = handler.asHandler().apply(null, request);
        ListResourcesResult result = resultMono.block();
        
        assertNotNull(result);
        assertEquals(1, result.resources().size());
        assertNotNull(result.nextCursor());
    }
    
    @Test
    void testListPromptsPagination() {
        ListPromptsHandler handler = new ListPromptsHandler(meta, notificationSender, 1);
        
        Map<String, Object> request = new HashMap<>();
        request.put("cursor", null);
        
        Mono<ListPromptsResult> resultMono = handler.asHandler().apply(null, request);
        ListPromptsResult result = resultMono.block();
        
        assertNotNull(result);
        assertEquals(1, result.prompts().size());
        assertNotNull(result.nextCursor());
    }
    
    @Test
    void testListToolsWithProgressToken() {
        ListToolsHandler handler = new ListToolsHandler(meta, notificationSender, 2);
        
        Map<String, Object> metaMap = new HashMap<>();
        metaMap.put("progressToken", "test-token-123");
        
        Map<String, Object> request = new HashMap<>();
        request.put("cursor", null);
        request.put("_meta", metaMap);
        
        Mono<ListToolsResult> resultMono = handler.asHandler().apply(null, request);
        ListToolsResult result = resultMono.block();
        
        assertNotNull(result);
        assertEquals(2, result.tools().size());
        assertFalse(notifications.isEmpty());
    }
    
    @Test
    void testPageCursorFirst() {
        PageCursor cursor = PageCursor.first();
        assertEquals(0, cursor.offset());
        assertEquals(50, cursor.limit());
    }
    
    @Test
    void testPageCursorNext() {
        PageCursor first = PageCursor.first();
        PageCursor second = first.next();
        
        assertEquals(50, second.offset());
        assertEquals(50, second.limit());
    }
    
    @Test
    void testPageCursorEncodeDecode() {
        PageCursor original = PageCursor.of(100, 25);
        String encoded = original.encode();
        PageCursor decoded = PageCursor.parse(encoded);
        
        assertEquals(100, decoded.offset());
        assertEquals(25, decoded.limit());
    }
    
    @Test
    void testPaginatedResult() {
        List<String> items = List.of("a", "b", "c");
        PaginatedResult<String> result = PaginatedResult.of(items, "next-cursor");
        
        assertEquals(3, result.items().size());
        assertEquals("next-cursor", result.nextCursor());
        assertTrue(result.hasMore());
    }
    
    @Test
    void testPaginatedResultNoMore() {
        List<String> items = List.of("a", "b");
        PaginatedResult<String> result = PaginatedResult.of(items);
        
        assertEquals(2, result.items().size());
        assertNull(result.nextCursor());
        assertFalse(result.hasMore());
    }
}
