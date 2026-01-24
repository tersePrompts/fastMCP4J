//package com.ultrathink.fastmcp.openapi;
//
//import com.ultrathink.fastmcp.annotations.McpServer;
//import com.ultrathink.fastmcp.annotations.McpTool;
//import com.ultrathink.fastmcp.core.FastMCP;
//import com.ultrathink.fastmcp.scanner.AnnotationScanner;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@McpServer(name = "TestServer", version = "1.0.0", instructions = "Test instructions")
//class TestServer {
//    @McpTool(description = "Add two numbers")
//    public int add(int a, int b) {
//        return a + b;
//    }
//
//    @McpTool(description = "Multiply two numbers")
//    public int multiply(int x, int y) {
//        return x * y;
//    }
//}
//
//class OpenApiTest {
//
//    @Test
//    void testGenerateOpenApi() {
//        String json = FastMCP.server(TestServer.class).generateOpenApi();
//        assertNotNull(json);
//        assertTrue(json.contains("TestServer API"));
//        assertTrue(json.contains("1.0.0"));
//        assertTrue(json.contains("/tools/add"));
//        assertTrue(json.contains("/tools/multiply"));
//        assertTrue(json.contains("Add two numbers"));
//        assertTrue(json.contains("Multiply two numbers"));
//    }
//
//    @Test
//    void testOpenApiSpec() {
//        OpenApiGenerator generator = new OpenApiGenerator();
//        String json = generator.toJson(new AnnotationScanner().scan(TestServer.class));
//        assertNotNull(json);
//        assertTrue(json.contains("\"openapi\""));
//        assertTrue(json.contains("\"info\""));
//        assertTrue(json.contains("\"paths\""));
//    }
//}
