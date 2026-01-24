package io.github.fastmcp.adapter;

import io.github.fastmcp.annotations.*;
import io.github.fastmcp.model.*;
import io.github.fastmcp.scanner.AnnotationScanner;
import io.modelcontextprotocol.spec.McpSchema;
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
        
        McpSchema.CallToolResult result = marshaller.marshal("test message");
        
        assertFalse(result.isError());
        assertEquals(1, result.content().size());
        assertEquals("test message", ((McpSchema.TextContent) result.content().get(0)).text());
    }

    @Test
    void testResponseMarshallingNull() {
        ResponseMarshaller marshaller = new ResponseMarshaller();

        McpSchema.CallToolResult result = marshaller.marshal(null);

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
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("testTool", args);
        
        Object exchange = new Object();
        Mono<McpSchema.CallToolResult> resultMono = handler.asHandler().apply(exchange, request);

        McpSchema.CallToolResult result = resultMono.block();
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals("echo: hello", ((McpSchema.TextContent) result.content().get(0)).text());
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
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("testTool", args);
        
        Object exchange = new Object();
        Mono<McpSchema.CallToolResult> resultMono = handler.asHandler().apply(exchange, request);

        McpSchema.CallToolResult result = resultMono.block();
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals("async: hello", ((McpSchema.TextContent) result.content().get(0)).text());
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
        args.put("a", 3);  // Missing 'b' parameter
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("testTool", args);
        
        Object exchange = new Object();
        Mono<McpSchema.CallToolResult> resultMono = handler.asHandler().apply(exchange, request);

        McpSchema.CallToolResult result = resultMono.block();
        assertNotNull(result);
        assertTrue(result.isError());
        assertNotNull(((McpSchema.TextContent) result.content().get(0)).text());
    }
}
