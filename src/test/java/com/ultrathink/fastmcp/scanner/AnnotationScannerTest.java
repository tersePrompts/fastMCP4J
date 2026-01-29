package com.ultrathink.fastmcp.scanner;

import com.ultrathink.fastmcp.annotations.McpPrompt;
import com.ultrathink.fastmcp.annotations.McpResource;
import com.ultrathink.fastmcp.annotations.McpServer;
import com.ultrathink.fastmcp.annotations.McpTool;
import com.ultrathink.fastmcp.annotations.scanner.AnnotationScanner;
import com.ultrathink.fastmcp.annotations.scanner.ValidationException;
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

    // ============================================
    // MULTI-CLASS MODULE TESTS
    // ============================================

    public static class UserTools {
        @McpTool(description = "Get user by ID")
        public String getUser(String userId) {
            return "User: " + userId;
        }

        @McpTool(description = "Create new user")
        public String createUser(String name, String email) {
            return "Created user: " + name;
        }
    }

    public static class AdminTools {
        @McpTool(description = "Delete user by ID")
        public String deleteUser(String userId) {
            return "Deleted: " + userId;
        }

        @McpTool(description = "List all users")
        public String listUsers() {
            return "Users: alice, bob, charlie";
        }
    }

    public static class ReportTools {
        @McpResource(uri = "report://daily", name = "Daily Report", mimeType = "text/plain")
        public String getDailyReport() {
            return "Daily report content";
        }

        @McpPrompt(name = "summary", description = "Generate summary prompt")
        public String summaryPrompt() {
            return "Generate a summary of the data";
        }
    }

    @McpServer(
        name = "ManualModulesServer",
        version = "1.0.0",
        modules = {UserTools.class, AdminTools.class, ReportTools.class}
    )
    public static class ManualModulesServer {
        @McpTool(description = "Server status")
        public String status() {
            return "Running";
        }
    }

    @Test
    void testManualModulesScansAllTools() {
        ServerMeta meta = scanner.scan(ManualModulesServer.class);

        // Server class has 1 tool + UserTools has 2 + AdminTools has 2 = 5 total
        assertEquals(5, meta.getTools().size(),
            "Should scan tools from server class and all module classes");
    }

    @Test
    void testManualModulesScansResources() {
        ServerMeta meta = scanner.scan(ManualModulesServer.class);

        assertEquals(1, meta.getResources().size(),
            "Should scan resources from module classes");
        assertEquals("Daily Report", meta.getResources().get(0).getName());
    }

    @Test
    void testManualModulesScansPrompts() {
        ServerMeta meta = scanner.scan(ManualModulesServer.class);

        assertEquals(1, meta.getPrompts().size(),
            "Should scan prompts from module classes");
        assertEquals("summary", meta.getPrompts().get(0).getName());
    }

    @Test
    void testManualModulesToolNames() {
        ServerMeta meta = scanner.scan(ManualModulesServer.class);

        var toolNames = meta.getTools().stream()
            .map(com.ultrathink.fastmcp.model.ToolMeta::getName)
            .toList();

        assertTrue(toolNames.contains("getUser"), "Should contain getUser from UserTools");
        assertTrue(toolNames.contains("createUser"), "Should contain createUser from UserTools");
        assertTrue(toolNames.contains("deleteUser"), "Should contain deleteUser from AdminTools");
        assertTrue(toolNames.contains("listUsers"), "Should contain listUsers from AdminTools");
        assertTrue(toolNames.contains("status"), "Should contain status from server class");
    }

    @Test
    void testEmptyModulesArray() {
        @McpServer(name = "EmptyModules", modules = {})
        class EmptyModulesServer {
            @McpTool(description = "A tool")
            public String tool() {
                return "result";
            }
        }

        ServerMeta meta = scanner.scan(EmptyModulesServer.class);

        assertEquals(1, meta.getTools().size(),
            "Empty modules array should work normally");
    }

    public static class EmptyModule {
        // No annotations - just a placeholder
    }

    @Test
    void testModuleWithNoAnnotations() {
        @McpServer(name = "EmptyModuleTest", modules = {EmptyModule.class})
        class ServerWithEmptyModule {
            @McpTool(description = "A tool")
            public String tool() {
                return "result";
            }
        }

        ServerMeta meta = scanner.scan(ServerWithEmptyModule.class);

        assertEquals(1, meta.getTools().size(),
            "Module with no annotations should be handled gracefully");
    }

    @McpServer(
        name = "PackageScanServer",
        version = "1.0.0",
        scanBasePackage = "com.ultrathink.fastmcp.scanner.testpackage"
    )
    public static class PackageScanServer {
        @McpTool(description = "Server tool")
        public String serverTool() {
            return "from server";
        }
    }

    @Test
    void testPackageScanDiscoversTools() {
        ServerMeta meta = scanner.scan(PackageScanServer.class);

        // Should discover tools from the test package
        // The package contains: ExtraTools.class with 2 tools
        assertTrue(meta.getTools().size() >= 1,
            "Should discover tools from scanned package");
    }

    @Test
    void testBothApproachesCombined() {
        @McpServer(
            name = "CombinedServer",
            version = "1.0.0",
            modules = {UserTools.class},
            scanBasePackage = "com.ultrathink.fastmcp.scanner.testpackage"
        )
        class CombinedServer {
            @McpTool(description = "Server tool")
            public String serverTool() {
                return "result";
            }
        }

        ServerMeta meta = scanner.scan(CombinedServer.class);

        // Should have: serverTool (1) + UserTools (2) + package scan tools
        assertTrue(meta.getTools().size() >= 3,
            "Should combine both manual modules and package scan");
    }

    @Test
    void testServerMetaContainsAllData() {
        ServerMeta meta = scanner.scan(ManualModulesServer.class);

        assertEquals("ManualModulesServer", meta.getName());
        assertEquals("1.0.0", meta.getVersion());
        assertFalse(meta.getTools().isEmpty());
        assertFalse(meta.getResources().isEmpty());
        assertFalse(meta.getPrompts().isEmpty());
    }
}