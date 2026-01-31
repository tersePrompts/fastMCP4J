package io.github.terseprompts.fastmcp.hook;

import io.github.terseprompts.fastmcp.annotations.*;
import io.github.terseprompts.fastmcp.model.ToolMeta;
import io.github.terseprompts.fastmcp.annotations.scanner.AnnotationScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HookManagerTest {

    @McpServer(name = "TestServer")
    public static class TestServer {
        private final List<String> executionLog = new ArrayList<>();

        @McpTool(description = "Test tool")
        public String testTool(String input) {
            executionLog.add("tool:" + input);
            return "result:" + input;
        }

        @McpPreHook(toolName = "testTool")
        public void preHook(Map<String, Object> args) {
            executionLog.add("pre:" + args.get("input"));
        }

        @McpPostHook(toolName = "testTool")
        public void postHook(Map<String, Object> args, Object result) {
            executionLog.add("post:" + result);
        }

        @McpPreHook(toolName = "*")
        public void globalPre(Map<String, Object> args) {
            executionLog.add("globalPre");
        }

        @McpPostHook(toolName = "*")
        public void globalPost(Map<String, Object> args, Object result) {
            executionLog.add("globalPost");
        }

        public List<String> getExecutionLog() {
            return executionLog;
        }
    }

    private TestServer server;
    private HookManager hookManager;

    @BeforeEach
    void setUp() {
        server = new TestServer();
        AnnotationScanner scanner = new AnnotationScanner();
        List<ToolMeta> tools = scanner.scan(TestServer.class).getTools();
        hookManager = new HookManager(server, tools);
    }

    @Test
    void testPreHooksExecuted() {
        Map<String, Object> args = Map.of("input", "test");

        hookManager.executePreHooks("testTool", args);

        List<String> log = server.getExecutionLog();
        assertTrue(log.contains("globalPre"), "Global pre-hook should execute");
        assertTrue(log.contains("pre:test"), "Tool-specific pre-hook should execute");
    }

    @Test
    void testPostHooksExecuted() {
        Map<String, Object> args = Map.of("input", "test");
        Object result = "result:test";

        hookManager.executePostHooks("testTool", args, result);

        List<String> log = server.getExecutionLog();
        assertTrue(log.contains("globalPost"), "Global post-hook should execute");
        assertTrue(log.contains("post:result:test"), "Tool-specific post-hook should execute");
    }

    @Test
    void testHookOrdering() {
        Map<String, Object> args = Map.of("input", "test");

        hookManager.executePreHooks("testTool", args);
        hookManager.executePostHooks("testTool", args, "result");

        List<String> log = server.getExecutionLog();
        int preIndex = log.indexOf("pre:test");
        int globalPreIndex = log.indexOf("globalPre");

        assertTrue(preIndex >= 0 && globalPreIndex >= 0,
                "Both pre-hooks should execute");
    }

    @Test
    void testGlobalHooksExecuteForAllTools() {
        Map<String, Object> args = Map.of("input", "test");

        // Execute hooks for a non-existent tool - global hooks should still fire
        hookManager.executePreHooks("nonExistentTool", args);

        List<String> log = server.getExecutionLog();
        assertTrue(log.contains("globalPre"),
                "Global hooks should execute for any tool");
    }

    @Test
    void testHooksDoNotExecuteForWrongTool() {
        Map<String, Object> args = Map.of("input", "test");

        hookManager.executePreHooks("otherTool", args);

        List<String> log = server.getExecutionLog();
        assertFalse(log.contains("pre:test"),
                "Tool-specific hooks should not execute for different tool");
        assertTrue(log.contains("globalPre"),
                "Global hooks should still execute");
    }

    @McpServer(name = "PriorityServer")
    public static class PriorityTestServer {
        private final List<String> executionLog = new ArrayList<>();

        @McpTool(description = "Test tool")
        public String testTool(String input) {
            return "result";
        }

        @McpPreHook(toolName = "*", order = 3)
        public void preOrder3(Map<String, Object> args) {
            executionLog.add("pre:3");
        }

        @McpPreHook(toolName = "*", order = 1)
        public void preOrder1(Map<String, Object> args) {
            executionLog.add("pre:1");
        }

        @McpPreHook(toolName = "*", order = 2)
        public void preOrder2(Map<String, Object> args) {
            executionLog.add("pre:2");
        }

        public List<String> getExecutionLog() {
            return executionLog;
        }
    }

    @Test
    void testHookExecutionOrder() {
        PriorityTestServer server = new PriorityTestServer();
        AnnotationScanner scanner = new AnnotationScanner();
        List<ToolMeta> tools = scanner.scan(PriorityTestServer.class).getTools();
        HookManager priorityHookManager = new HookManager(server, tools);

        Map<String, Object> args = Map.of("input", "test");
        priorityHookManager.executePreHooks("testTool", args);

        List<String> log = server.getExecutionLog();
        assertEquals(List.of("pre:1", "pre:2", "pre:3"), log,
                "Hooks should execute in order (1, 2, 3)");
    }

    @McpServer(name = "HookFailureServer")
    public static class HookFailureTestServer {
        private final List<String> executionLog = new ArrayList<>();

        @McpTool(description = "Test tool")
        public String testTool(String input) {
            return "result";
        }

        @McpPreHook(toolName = "*", order = 1)
        public void failingHook(Map<String, Object> args) {
            executionLog.add("before-failure");
            throw new RuntimeException("Hook failed!");
        }

        @McpPreHook(toolName = "*", order = 2)
        public void afterFailure(Map<String, Object> args) {
            executionLog.add("after-failure");
        }

        public List<String> getExecutionLog() {
            return executionLog;
        }
    }

    @Test
    void testHookFailureBehavior_WARN() {
        HookFailureTestServer server = new HookFailureTestServer();
        AnnotationScanner scanner = new AnnotationScanner();
        List<ToolMeta> tools = scanner.scan(HookFailureTestServer.class).getTools();
        HookManager failureHookManager = new HookManager(server, tools);
        failureHookManager.setFailureMode(HookManager.HookFailureMode.WARN);

        Map<String, Object> args = Map.of("input", "test");
        failureHookManager.executePreHooks("testTool", args);

        List<String> log = server.getExecutionLog();
        assertTrue(log.contains("before-failure"), "Failing hook should execute");
        assertTrue(log.contains("after-failure"), "Later hooks should still execute in WARN mode");
    }

    @Test
    void testHookFailureBehavior_STRICT() {
        HookFailureTestServer server = new HookFailureTestServer();
        AnnotationScanner scanner = new AnnotationScanner();
        List<ToolMeta> tools = scanner.scan(HookFailureTestServer.class).getTools();
        HookManager failureHookManager = new HookManager(server, tools);
        failureHookManager.setFailureMode(HookManager.HookFailureMode.STRICT);

        Map<String, Object> args = Map.of("input", "test");

        assertThrows(RuntimeException.class, () -> {
            failureHookManager.executePreHooks("testTool", args);
        }, "STRICT mode should throw exception on hook failure");
    }
}
