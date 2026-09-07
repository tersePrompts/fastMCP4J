package io.github.terseprompts.fastmcp.annotations;

import io.github.terseprompts.fastmcp.context.Context;
import io.github.terseprompts.fastmcp.context.ContextImpl;
import io.github.terseprompts.fastmcp.core.Injector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@code @McpOnStartup} / {@code @McpOnShutdown} lifecycle methods
 * (Feature 4), driven through {@link Injector}.
 */
class LifecycleTest {

    @AfterEach
    void clearContext() {
        ContextImpl.clearCurrentContext();
    }

    static class LifecycleServer {
        final List<String> calls = new ArrayList<>();
        Context capturedContext;

        @McpOnStartup
        void startup() {
            calls.add("startup");
        }

        @McpOnStartup
        void startupWithContext(Context ctx) {
            calls.add("startup-ctx");
            capturedContext = ctx;
        }

        @McpOnShutdown
        void shutdown() {
            calls.add("shutdown");
        }
    }

    @Test
    void testStartupMethodsInvokedInOrder() {
        LifecycleServer server = new LifecycleServer();
        Injector.invokeStartup(server);
        assertTrue(server.calls.containsAll(List.of("startup", "startup-ctx")), "Both startup methods run");
        assertEquals(2, server.calls.size(), "Both startup methods run exactly once");
    }

    @Test
    void testStartupWithoutContextUsesNoArg() {
        LifecycleServer server = new LifecycleServer();
        ContextImpl.clearCurrentContext();
        Injector.invokeStartup(server);
        assertNull(server.capturedContext);
        assertTrue(server.calls.containsAll(List.of("startup", "startup-ctx")), "Both startup methods run");
    }

    @Test
    void testStartupWithContextReceivesCurrentContext() {
        ContextImpl context = new ContextImpl(
                new ContextImpl.RequestContext("r", "c", "s", "stdio", Map.of()),
                "Test", new ConcurrentHashMap<>(),
                new ContextImpl.NotificationHelper() {
                    @Override public void sendLog(String level, String message) {}
                    @Override public void sendProgress(double progress, double total, String message) {}
                    @Override public void sendResourceChange(String action, String uri) {}
                });
        ContextImpl.setCurrentContext(context);

        LifecycleServer server = new LifecycleServer();
        Injector.invokeStartup(server);
        assertSame(context, server.capturedContext);
    }

    @Test
    void testShutdownMethodsInvoked() {
        LifecycleServer server = new LifecycleServer();
        Injector.invokeShutdown(server);
        assertEquals(List.of("shutdown"), server.calls);
    }

    @Test
    void testNullTargetIsSafe() {
        Injector.invokeStartup(null);
        Injector.invokeShutdown(null);
        assertTrue(true);
    }

    // ============================================
    // Combined: injection then startup
    // ============================================

    static class InjectedServer {
        @McpInject
        Service service;
        @McpOnStartup
        void startup() {
            if (service != null) {
                service.started = true;
            }
        }
    }

    static class Service {
        boolean started = false;
    }

    @Test
    void testInjectionRunsBeforeStartup() {
        Service service = new Service();
        InjectedServer server = new InjectedServer();

        // Simulate the builder flow: inject then startup.
        io.github.terseprompts.fastmcp.core.InjectionRegistry registry =
                new io.github.terseprompts.fastmcp.core.InjectionRegistry()
                        .register("service", service);
        Injector.injectFields(server, registry);
        Injector.invokeStartup(server);

        assertSame(service, server.service);
        assertTrue(service.started, "Startup should see the injected dependency");
    }
}