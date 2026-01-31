package io.github.terseprompts.fastmcp.adapter;

import io.github.terseprompts.fastmcp.annotations.McpAsync;
import io.github.terseprompts.fastmcp.annotations.McpPrompt;
import io.github.terseprompts.fastmcp.annotations.McpServer;
import io.github.terseprompts.fastmcp.model.PromptMeta;
import io.github.terseprompts.fastmcp.model.ServerMeta;
import io.github.terseprompts.fastmcp.annotations.scanner.AnnotationScanner;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PromptHandler.
 */
class PromptHandlerTest {

    @McpServer(name = "TestPromptServer", version = "1.0.0")
    public static class TestPromptServer {
        @McpPrompt(name = "greet", description = "Greet the user")
        public String greet(String name) {
            return "Hello, " + name + "!";
        }

        @McpPrompt(name = "async_prompt", description = "Async prompt")
        @McpAsync
        public Mono<String> asyncPrompt(String topic) {
            return Mono.just("Tell me about " + topic);
        }

        @McpPrompt(name = "error_prompt", description = "Error prompt")
        public String errorPrompt(String input) {
            throw new RuntimeException("Prompt generation failed");
        }

        @McpPrompt(name = "multi_message", description = "Multi message prompt")
        public List<McpSchema.PromptMessage> multiMessagePrompt(String context) {
            return List.of(
                    new McpSchema.PromptMessage(McpSchema.Role.USER,
                            new McpSchema.TextContent("Context: " + context)),
                    new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT,
                            new McpSchema.TextContent("How can I help?"))
            );
        }
    }

    @Test
    void testSyncPromptInvocation() throws Exception {
        TestPromptServer server = new TestPromptServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestPromptServer.class);

        PromptMeta promptMeta = meta.getPrompts().stream()
                .filter(p -> p.getName().equals("greet"))
                .findFirst()
                .orElseThrow();

        PromptHandler handler = new PromptHandler(
                server,
                promptMeta,
                new ArgumentBinder(),
                new PromptResponseMarshaller()
        );

        Map<String, Object> args = Map.of("name", "Alice");
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("greet", args);
        Mono<McpSchema.GetPromptResult> resultMono = handler.asHandler().apply(null, request);

        McpSchema.GetPromptResult result = resultMono.block();
        assertNotNull(result);
        assertNotNull(result.description());
        assertEquals(1, result.messages().size());

        McpSchema.PromptMessage message = result.messages().get(0);
        assertEquals(McpSchema.Role.USER, message.role());
        McpSchema.TextContent content = (McpSchema.TextContent) message.content();
        assertEquals("Hello, Alice!", content.text());
    }

    @Test
    void testAsyncPromptInvocation() throws Exception {
        TestPromptServer server = new TestPromptServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestPromptServer.class);

        PromptMeta promptMeta = meta.getPrompts().stream()
                .filter(p -> p.getName().equals("async_prompt"))
                .findFirst()
                .orElseThrow();

        PromptHandler handler = new PromptHandler(
                server,
                promptMeta,
                new ArgumentBinder(),
                new PromptResponseMarshaller()
        );

        Map<String, Object> args = Map.of("topic", "AI");
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("async_prompt", args);
        Mono<McpSchema.GetPromptResult> resultMono = handler.asHandler().apply(null, request);

        McpSchema.GetPromptResult result = resultMono.block();
        assertNotNull(result);
        assertEquals(1, result.messages().size());

        McpSchema.PromptMessage message = result.messages().get(0);
        assertEquals(McpSchema.Role.USER, message.role());
        McpSchema.TextContent content = (McpSchema.TextContent) message.content();
        assertEquals("Tell me about AI", content.text());
    }

    @Test
    void testPromptErrorHandling() throws Exception {
        TestPromptServer server = new TestPromptServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestPromptServer.class);

        PromptMeta promptMeta = meta.getPrompts().stream()
                .filter(p -> p.getName().equals("error_prompt"))
                .findFirst()
                .orElseThrow();

        PromptHandler handler = new PromptHandler(
                server,
                promptMeta,
                new ArgumentBinder(),
                new PromptResponseMarshaller()
        );

        Map<String, Object> args = Map.of("input", "test");
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("error_prompt", args);
        Mono<McpSchema.GetPromptResult> resultMono = handler.asHandler().apply(null, request);

        McpSchema.GetPromptResult result = resultMono.block();
        assertNotNull(result);
        assertNotNull(result.description());
        assertTrue(result.description().contains("Error occurred"));

        assertEquals(1, result.messages().size());
        McpSchema.PromptMessage message = result.messages().get(0);
        McpSchema.TextContent content = (McpSchema.TextContent) message.content();
        assertTrue(content.text().contains("Error:"));
        // The exception message may be wrapped in InvocationTargetException or other wrappers
        // Just check that we got an error message
        assertNotNull(content.text());
        assertFalse(content.text().isEmpty());
    }

    @Test
    void testMultiMessagePromptInvocation() throws Exception {
        TestPromptServer server = new TestPromptServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestPromptServer.class);

        PromptMeta promptMeta = meta.getPrompts().stream()
                .filter(p -> p.getName().equals("multi_message"))
                .findFirst()
                .orElseThrow();

        PromptHandler handler = new PromptHandler(
                server,
                promptMeta,
                new ArgumentBinder(),
                new PromptResponseMarshaller()
        );

        Map<String, Object> args = Map.of("context", "Project setup");
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("multi_message", args);
        Mono<McpSchema.GetPromptResult> resultMono = handler.asHandler().apply(null, request);

        McpSchema.GetPromptResult result = resultMono.block();
        assertNotNull(result);
        assertEquals(2, result.messages().size());

        McpSchema.PromptMessage firstMessage = result.messages().get(0);
        assertEquals(McpSchema.Role.USER, firstMessage.role());
        McpSchema.TextContent firstContent = (McpSchema.TextContent) firstMessage.content();
        assertEquals("Context: Project setup", firstContent.text());

        McpSchema.PromptMessage secondMessage = result.messages().get(1);
        assertEquals(McpSchema.Role.ASSISTANT, secondMessage.role());
        McpSchema.TextContent secondContent = (McpSchema.TextContent) secondMessage.content();
        assertEquals("How can I help?", secondContent.text());
    }

    @Test
    void testPromptWithNoArguments() throws Exception {
        TestPromptServer server = new TestPromptServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestPromptServer.class);

        // Find a prompt that takes no arguments (none in this test, but we test empty args)
        PromptMeta promptMeta = meta.getPrompts().stream()
                .filter(p -> p.getName().equals("greet"))
                .findFirst()
                .orElseThrow();

        PromptHandler handler = new PromptHandler(
                server,
                promptMeta,
                new ArgumentBinder(),
                new PromptResponseMarshaller()
        );

        // Empty arguments - will cause binding to use null/default
        Map<String, Object> args = Map.of();
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("greet", args);
        Mono<McpSchema.GetPromptResult> resultMono = handler.asHandler().apply(null, request);

        // Should complete without exception (though may have null argument)
        McpSchema.GetPromptResult result = resultMono.block();
        assertNotNull(result);
    }
}
