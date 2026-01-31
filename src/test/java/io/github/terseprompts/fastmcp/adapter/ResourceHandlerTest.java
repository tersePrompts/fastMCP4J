package io.github.terseprompts.fastmcp.adapter;

import io.github.terseprompts.fastmcp.annotations.McpAsync;
import io.github.terseprompts.fastmcp.annotations.McpResource;
import io.github.terseprompts.fastmcp.annotations.McpServer;
import io.github.terseprompts.fastmcp.model.ResourceMeta;
import io.github.terseprompts.fastmcp.model.ServerMeta;
import io.github.terseprompts.fastmcp.annotations.scanner.AnnotationScanner;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ResourceHandler.
 */
class ResourceHandlerTest {

    @McpServer(name = "TestResourceServer", version = "1.0.0")
    public static class TestResourceServer {
        @McpResource(uri = "file:///test.txt", name = "test", description = "Test resource")
        public String getTextResource() {
            return "Hello, World!";
        }

        @McpResource(uri = "file:///async.txt", name = "async", description = "Async resource")
        @McpAsync
        public Mono<String> getAsyncResource() {
            return Mono.just("Async content");
        }

        @McpResource(uri = "file:///error.txt", name = "error", description = "Error resource")
        public String getErrorResource() {
            throw new RuntimeException("Resource error");
        }

        @McpResource(uri = "file:///binary.txt", name = "binary", description = "Binary resource")
        public byte[] getBinaryResource() {
            return new byte[]{0x01, 0x02, 0x03};
        }
    }

    @Test
    void testSyncResourceInvocation() throws Exception {
        TestResourceServer server = new TestResourceServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestResourceServer.class);

        ResourceMeta resourceMeta = meta.getResources().stream()
                .filter(r -> r.getName().equals("test"))
                .findFirst()
                .orElseThrow();

        ResourceHandler handler = new ResourceHandler(
                server,
                resourceMeta,
                new ArgumentBinder(),
                new ResourceResponseMarshaller()
        );

        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("file:///test.txt");
        Mono<McpSchema.ReadResourceResult> resultMono = handler.asHandler().apply(null, request);

        McpSchema.ReadResourceResult result = resultMono.block();
        assertNotNull(result);
        assertEquals(1, result.contents().size());

        // ResourceResponseMarshaller uses BlobResourceContents with base64 encoding
        McpSchema.BlobResourceContents content =
                (McpSchema.BlobResourceContents) result.contents().get(0);
        assertNotNull(content.blob());

        // Decode and verify the content
        String decoded = new String(java.util.Base64.getDecoder().decode(content.blob()));
        assertEquals("Hello, World!", decoded);
    }

    @Test
    void testAsyncResourceInvocation() throws Exception {
        TestResourceServer server = new TestResourceServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestResourceServer.class);

        ResourceMeta resourceMeta = meta.getResources().stream()
                .filter(r -> r.getName().equals("async"))
                .findFirst()
                .orElseThrow();

        ResourceHandler handler = new ResourceHandler(
                server,
                resourceMeta,
                new ArgumentBinder(),
                new ResourceResponseMarshaller()
        );

        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("file:///async.txt");
        Mono<McpSchema.ReadResourceResult> resultMono = handler.asHandler().apply(null, request);

        McpSchema.ReadResourceResult result = resultMono.block();
        assertNotNull(result);
        assertEquals(1, result.contents().size());

        McpSchema.BlobResourceContents content =
                (McpSchema.BlobResourceContents) result.contents().get(0);
        assertNotNull(content.blob());

        String decoded = new String(java.util.Base64.getDecoder().decode(content.blob()));
        assertEquals("Async content", decoded);
    }

    @Test
    void testResourceErrorHandling() throws Exception {
        TestResourceServer server = new TestResourceServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestResourceServer.class);

        ResourceMeta resourceMeta = meta.getResources().stream()
                .filter(r -> r.getName().equals("error"))
                .findFirst()
                .orElseThrow();

        ResourceHandler handler = new ResourceHandler(
                server,
                resourceMeta,
                new ArgumentBinder(),
                new ResourceResponseMarshaller()
        );

        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("file:///error.txt");
        Mono<McpSchema.ReadResourceResult> resultMono = handler.asHandler().apply(null, request);

        McpSchema.ReadResourceResult result = resultMono.block();
        assertNotNull(result);
        assertEquals(1, result.contents().size());

        // Error results are returned as BlobResourceContents with base64 encoded error message
        McpSchema.BlobResourceContents content =
                (McpSchema.BlobResourceContents) result.contents().get(0);
        assertNotNull(content.blob());

        String decoded = new String(java.util.Base64.getDecoder().decode(content.blob()));
        // Just verify we got an error message (it may be wrapped in different exception types)
        assertNotNull(decoded);
        assertFalse(decoded.isEmpty());
    }

    @Test
    void testBinaryResourceInvocation() throws Exception {
        TestResourceServer server = new TestResourceServer();
        AnnotationScanner scanner = new AnnotationScanner();
        ServerMeta meta = scanner.scan(TestResourceServer.class);

        ResourceMeta resourceMeta = meta.getResources().stream()
                .filter(r -> r.getName().equals("binary"))
                .findFirst()
                .orElseThrow();

        ResourceHandler handler = new ResourceHandler(
                server,
                resourceMeta,
                new ArgumentBinder(),
                new ResourceResponseMarshaller()
        );

        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("file:///binary.txt");
        Mono<McpSchema.ReadResourceResult> resultMono = handler.asHandler().apply(null, request);

        McpSchema.ReadResourceResult result = resultMono.block();
        assertNotNull(result);
        assertEquals(1, result.contents().size());

        McpSchema.BlobResourceContents content =
                (McpSchema.BlobResourceContents) result.contents().get(0);
        assertNotNull(content.blob());

        // Verify the binary content
        byte[] decoded = java.util.Base64.getDecoder().decode(content.blob());
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, decoded);
    }
}
