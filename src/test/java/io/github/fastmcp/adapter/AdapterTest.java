package io.github.fastmcp.adapter;

import io.github.fastmcp.annotations.*;
import io.github.fastmcp.model.*;
import io.github.fastmcp.scanner.AnnotationScanner;
import io.modelcontextprotocol.spec.McpSchema.*;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.lang.reflect.Method;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

class AdapterTest {

    @McpServer(name = "TestServer")
    public static class TestServer {
        @McpTool(description = "Echo tool")
        public String echo(String message) {
            return "echo: " + message;
        }

        @McpTool(description = "Async tool")
        @McpAsync
        public Mono<String> asyncEcho(String message) {
            return Mono.just("async: " + message);
        }

        @McpTool(description = "Add numbers")
        public int add(int a, int b) {
            return a + b;
        }

        @McpResource(uri = "file://config.txt", description = "Config file")
        public String getConfig() {
            return "config: value";
        }

        @McpResource(uri = "file://status.txt", description = "Status file")
        @McpAsync
        public Mono<String> getStatus() {
            return Mono.just("status: ok");
        }

        @McpPrompt(description = "System prompt")
        public String getSystemPrompt() {
            return "You are a helpful assistant.";
        }

        @McpPrompt(description = "Welcome prompt")
        @McpAsync
        public Mono<String> getWelcomePrompt() {
            return Mono.just("Welcome to the system!");
        }
    }

    @Test
    void testArgumentBinding() throws Exception {
        ArgumentBinder binder = new ArgumentBinder();
        Method method = TestServer.class.getMethod("add", int.class, int.class);
        
        Map<String, Object> args = new HashMap<>();
        args.put("a", 3);
        args.put("b", 5);
        
        Object[] bound = binder.bind(method, args);
        
        assertEquals(2, bound.length);
        assertEquals(3, bound[0]);
        assertEquals(5, bound[1]);
    }

    @Test
    void testResponseMarshalling() {
        ResponseMarshaller marshaller = new ResponseMarshaller();
        
        CallToolResult result = marshaller.marshal("test message");
        
        assertFalse(result.isError());
        assertEquals(1, result.content().size());
        assertTrue(result.content().get(0) instanceof TextContent);
        assertEquals("test message", ((TextContent) result.content().get(0)).text());
    }

    @Test
    void testResponseMarshallingNull() {
        ResponseMarshaller marshaller = new ResponseMarshaller();
        
        CallToolResult result = marshaller.marshal(null);
        
        assertFalse(result.isError());
        assertEquals(0, result.content().size());
    }

    @Test
    void testToolHandlerSyncInvocation() throws Exception {
        TestServer server = new TestServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestServer.class);
        
        ToolMeta toolMeta = meta.getTools().stream()
            .filter(t -> t.getName().equals("echo"))
            .findFirst()
            .orElseThrow();
        
        ToolHandler handler = new ToolHandler(
            server,
            toolMeta,
            new ArgumentBinder(),
            new ResponseMarshaller()
        );
        
        Map<String, Object> args = new HashMap<>();
        args.put("message", "hello");
        CallToolRequest request = new CallToolRequest("echo", args);
        
        Mono<CallToolResult> resultMono = handler.asHandler().apply(null, request);
        
        CallToolResult result = resultMono.block();
        assertNotNull(result);
        assertFalse(result.isError());
        assertTrue(result.content().get(0) instanceof TextContent);
        assertEquals("echo: hello", ((TextContent) result.content().get(0)).text());
    }

    @Test
    void testToolHandlerAsyncInvocation() throws Exception {
        TestServer server = new TestServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestServer.class);
        
        ToolMeta toolMeta = meta.getTools().stream()
            .filter(t -> t.getName().equals("asyncEcho"))
            .findFirst()
            .orElseThrow();
        
        ToolHandler handler = new ToolHandler(
            server,
            toolMeta,
            new ArgumentBinder(),
            new ResponseMarshaller()
        );
        
        Map<String, Object> args = new HashMap<>();
        args.put("message", "hello");
        CallToolRequest request = new CallToolRequest("asyncEcho", args);
        
        Mono<CallToolResult> resultMono = handler.asHandler().apply(null, request);
        
        CallToolResult result = resultMono.block();
        assertNotNull(result);
        assertFalse(result.isError());
        assertTrue(result.content().get(0) instanceof TextContent);
        assertEquals("async: hello", ((TextContent) result.content().get(0)).text());
    }

    @Test
    void testToolHandlerErrorHandling() throws Exception {
        TestServer server = new TestServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestServer.class);
        
        // Use 'add' method which has primitive int parameters
        ToolMeta toolMeta = meta.getTools().stream()
            .filter(t -> t.getName().equals("add"))
            .findFirst()
            .orElseThrow();
        
        ToolHandler handler = new ToolHandler(
            server,
            toolMeta,
            new ArgumentBinder(),
            new ResponseMarshaller()
        );
        
        // Missing required primitive argument should cause error
        Map<String, Object> args = new HashMap<>();
        args.put("a",3);  // Missing 'b' parameter
        CallToolRequest request = new CallToolRequest("add", args);
        
        Mono<CallToolResult> resultMono = handler.asHandler().apply(null, request);
        
        CallToolResult result = resultMono.block();
        assertNotNull(result);
        assertTrue(result.isError());
        assertTrue(result.content().get(0) instanceof TextContent);
        assertNotNull(((TextContent) result.content().get(0)).text());
    }

    @Test
    void testResourceHandlerSyncInvocation() throws Exception {
        TestServer server = new TestServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestServer.class);

        ResourceMeta resourceMeta = meta.getResources().stream()
            .filter(r -> r.getUri().equals("file://config.txt"))
            .findFirst()
            .orElseThrow();

        ResourceHandler handler = new ResourceHandler(
            server,
            resourceMeta,
            new ResponseMarshaller()
        );

        ReadResourceRequest request = new ReadResourceRequest("file://config.txt");

        Mono<ReadResourceResult> resultMono = handler.asHandler().apply(null, request);

        ReadResourceResult result = resultMono.block();
        assertNotNull(result);
        assertFalse(result.contents().isEmpty());
        assertTrue(result.contents().get(0) instanceof TextResourceContents);
        assertEquals("config: value", ((TextResourceContents) result.contents().get(0)).text());
    }

    @Test
    void testResourceHandlerAsyncInvocation() throws Exception {
        TestServer server = new TestServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestServer.class);

        ResourceMeta resourceMeta = meta.getResources().stream()
            .filter(r -> r.getUri().equals("file://status.txt"))
            .findFirst()
            .orElseThrow();

        ResourceHandler handler = new ResourceHandler(
            server,
            resourceMeta,
            new ResponseMarshaller()
        );

        ReadResourceRequest request = new ReadResourceRequest("file://status.txt");

        Mono<ReadResourceResult> resultMono = handler.asHandler().apply(null, request);

        ReadResourceResult result = resultMono.block();
        assertNotNull(result);
        assertFalse(result.contents().isEmpty());
        assertTrue(result.contents().get(0) instanceof TextResourceContents);
        assertEquals("status: ok", ((TextResourceContents) result.contents().get(0)).text());
    }

    @Test
    void testPromptHandlerSyncInvocation() throws Exception {
        TestServer server = new TestServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestServer.class);

        PromptMeta promptMeta = meta.getPrompts().stream()
            .filter(p -> p.getName().equals("getSystemPrompt"))
            .findFirst()
            .orElseThrow();

        PromptHandler handler = new PromptHandler(
            server,
            promptMeta
        );

        GetPromptRequest request = new GetPromptRequest("getSystemPrompt", Map.of());

        Mono<GetPromptResult> resultMono = handler.asHandler().apply(null, request);

        GetPromptResult result = resultMono.block();
        assertNotNull(result);
        assertNotNull(result.messages());
        assertFalse(result.messages().isEmpty());
        Content content = result.messages().get(0).content();
        assertTrue(content instanceof TextContent);
        assertEquals("You are a helpful assistant.", ((TextContent) content).text());
    }

    @Test
    void testPromptHandlerAsyncInvocation() throws Exception {
        TestServer server = new TestServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestServer.class);

        PromptMeta promptMeta = meta.getPrompts().stream()
            .filter(p -> p.getName().equals("getWelcomePrompt"))
            .findFirst()
            .orElseThrow();

        PromptHandler handler = new PromptHandler(
            server,
            promptMeta
        );

        GetPromptRequest request = new GetPromptRequest("getWelcomePrompt", Map.of());

        Mono<GetPromptResult> resultMono = handler.asHandler().apply(null, request);

        GetPromptResult result = resultMono.block();
        assertNotNull(result);
        assertNotNull(result.messages());
        assertFalse(result.messages().isEmpty());
        Content content = result.messages().get(0).content();
        assertTrue(content instanceof TextContent);
        TextContent textContent = (TextContent) content;
        String actualText = textContent.text();
        assertTrue(actualText.contains("Welcome"), "Expected welcome message but got: " + actualText);
        assertTrue(actualText.contains("system"), "Expected system message but got: " + actualText);
    }
}
