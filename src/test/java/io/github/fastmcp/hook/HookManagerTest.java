package io.github.fastmcp.hook;

import io.github.fastmcp.annotations.*;
import io.github.fastmcp.model.ToolMeta;
import io.github.fastmcp.scanner.AnnotationScanner;
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
        assertTrue(log.contains("globalPre"));
        assertTrue(log.contains("pre:test"));
    }
    
    @Test
    void testPostHooksExecuted() {
        Map<String, Object> args = Map.of("input", "test");
        Object result = "result:test";
        
        hookManager.executePostHooks("testTool", args, result);
        
        List<String> log = server.getExecutionLog();
        assertTrue(log.contains("globalPost"));
        assertTrue(log.contains("post:result:test"));
    }
    
    @Test
    void testHookOrdering() {
        Map<String, Object> args = Map.of("input", "test");
        
        hookManager.executePreHooks("testTool", args);
        hookManager.executePostHooks("testTool", args, "result");
        
        List<String> log = server.getExecutionLog();
        int preIndex = log.indexOf("pre:test");
        int globalPreIndex = log.indexOf("globalPre");
        
        assertTrue(preIndex >= 0 && globalPreIndex >= 0);
    }
}
