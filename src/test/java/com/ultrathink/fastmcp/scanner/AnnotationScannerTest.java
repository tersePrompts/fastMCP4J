package com.ultrathink.fastmcp.scanner;

import com.ultrathink.fastmcp.annotations.McpServer;
import com.ultrathink.fastmcp.annotations.McpTool;
import com.ultrathink.fastmcp.model.ServerMeta;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnnotationScannerTest {

    private final AnnotationScanner scanner = new AnnotationScanner();

    @McpServer(name = "TestServer", version = "1.0.0")
    public static class ValidServer {
        @McpTool(description = "Test tool")
        public String testTool(String input) {
            return input;
        }
    }

    @Test
    void testScanValidServer() {
        ServerMeta meta = scanner.scan(ValidServer.class);

        assertEquals("TestServer", meta.getName());
        assertEquals("1.0.0", meta.getVersion());
        assertEquals(1, meta.getTools().size());
    }

    @Test
    void testScanMissingAnnotation_ThrowsException() {
        class InvalidServer {}

        assertThrows(ValidationException.class, () -> scanner.scan(InvalidServer.class));
    }

    @Test
    void testToolNameDefaultsToMethodName() {
        @McpServer(name = "Test")
        class TestServer {
            @McpTool(description = "Test")
            public void myMethod() {}
        }

        ServerMeta meta = scanner.scan(TestServer.class);
        assertEquals("myMethod", meta.getTools().get(0).getName());
    }
}