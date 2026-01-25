package com.ultrathink.fastmcp.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ContextImpl and Context interface.
 */
class ContextImplTest {
    
    private Map<String, Object> sessionState;
    private ContextImpl.RequestContext requestContext;
    private ContextImpl.NotificationHelper notificationHelper;
    private ContextImpl context;
    
    @BeforeEach
    void setUp() {
        sessionState = new ConcurrentHashMap<>();
        requestContext = new ContextImpl.RequestContext(
            "test-request-123",
            "test-client-456",
            "test-session-789",
            "stdio",
            Map.of("user_id", "test-user", "trace_id", "trace-123")
        );
        
        notificationHelper = new ContextImpl.NotificationHelper() {
            private final List<String> logs = new ArrayList<>();
            private final List<String> progressList = new ArrayList<>();
            private final List<String> resources = new ArrayList<>();

            @Override
            public void sendLog(String level, String message) {
                logs.add("[" + level + "] " + message);
                System.err.println("[TEST LOG] " + level + ": " + message);
            }

            @Override
            public void sendProgress(double progress, double total, String message) {
                String p = message != null ?
                    progress + "/" + total + ": " + message :
                    progress + "/" + total;
                progressList.add(p);
                System.err.println("[TEST PROGRESS] " + p);
            }

            @Override
            public void sendResourceChange(String action, String uri) {
                resources.add(action + ": " + uri);
                System.err.println("[TEST RESOURCE] " + action + ": " + uri);
            }
        };
        
        context = new ContextImpl(requestContext, "TestServer", sessionState, notificationHelper);
    }
    
    @AfterEach
    void tearDown() {
        ContextImpl.clearCurrentContext();
    }
    
    @Test
    void testGetRequestId() {
        assertEquals("test-request-123", context.getRequestId());
    }
    
    @Test
    void testGetClientId() {
        assertEquals("test-client-456", context.getClientId());
    }
    
    @Test
    void testGetSessionId() {
        assertEquals("test-session-789", context.getSessionId());
    }
    
    @Test
    void testGetTransport() {
        assertEquals("stdio", context.getTransport());
    }
    
    @Test
    void testGetServerName() {
        assertEquals("TestServer", context.getServerName());
    }
    
    @Test
    void testSessionState_SetGetDelete() {
        // Initially null
        assertNull(context.getState("testKey"));
        
        // Set value
        context.setState("testKey", "testValue");
        assertEquals("testValue", context.getState("testKey"));
        
        // Update value
        context.setState("testKey", "updatedValue");
        assertEquals("updatedValue", context.getState("testKey"));
        
        // Delete value
        context.deleteState("testKey");
        assertNull(context.getState("testKey"));
    }
    
    @Test
    void testSessionState_MultipleKeys() {
        context.setState("key1", "value1");
        context.setState("key2", 123);
        context.setState("key3", List.of("a", "b", "c"));
        
        assertEquals("value1", context.getState("key1"));
        assertEquals(123, context.getState("key2"));
        assertEquals(List.of("a", "b", "c"), context.getState("key3"));
    }
    
    @Test
    void testGetSessionId_ThrowsWhenNull() {
        ContextImpl.RequestContext nullSessionContext = new ContextImpl.RequestContext(
            "req-1",
            "client-1",
            null, // null session ID
            "stdio",
            Map.of()
        );
        ContextImpl contextWithNullSession = new ContextImpl(
            nullSessionContext, "TestServer", sessionState, notificationHelper
        );
        
        assertThrows(IllegalStateException.class, contextWithNullSession::getSessionId);
    }
    
    @Test
    void testGetCurrentContext_ThreadLocal() {
        // Set context in ThreadLocal
        ContextImpl.setCurrentContext(context);
        
        // Retrieve from ThreadLocal
        Context retrieved = ContextImpl.getCurrentContext();
        assertSame(context, retrieved);
    }
    
    @Test
    void testGetCurrentContext_ThrowsWhenNotSet() {
        ContextImpl.clearCurrentContext();
        
        assertThrows(IllegalStateException.class, ContextImpl::getCurrentContext);
    }
    
    @Test
    void testClearCurrentContext() {
        ContextImpl.setCurrentContext(context);
        assertSame(context, ContextImpl.getCurrentContext());
        
        ContextImpl.clearCurrentContext();
        
        assertThrows(IllegalStateException.class, ContextImpl::getCurrentContext);
    }
}
