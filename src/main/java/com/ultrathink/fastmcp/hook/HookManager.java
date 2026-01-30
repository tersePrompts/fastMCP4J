package com.ultrathink.fastmcp.hook;

import com.ultrathink.fastmcp.agent.lifecycle.SessionLifecycle;
import com.ultrathink.fastmcp.annotations.McpPreHook;
import com.ultrathink.fastmcp.annotations.McpPostHook;
import com.ultrathink.fastmcp.annotations.McpSessionLifecycle;
import com.ultrathink.fastmcp.model.ToolMeta;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Manages pre and post hooks for MCP tools.
 *
 * Hooks allow you to execute code before and after tool invocations for:
 * - Logging and auditing
 * - Validation
 * - Authentication/authorization
 * - Metrics collection
 * - Side effects
 * <p>
 * <b>Security:</b> Hooks are loaded from the server instance only and validated
 * to prevent unauthorized access. Private hook methods are allowed for internal use.
 */
@Slf4j
public class HookManager {
    private final Object serverInstance;
    private final List<HookMethod> preHooks;
    private final List<HookMethod> postHooks;
    private final List<SessionLifecycleMethod> sessionLifecycleHooks;

    // Security: Only allow hooks from the same classloader as the server instance
    private final ClassLoader serverClassLoader;
    private SessionLifecycle sessionLifecycle;

    public HookManager(Object serverInstance, List<ToolMeta> tools) {
        this.serverInstance = serverInstance;
        this.serverClassLoader = serverInstance.getClass().getClassLoader();
        this.preHooks = new ArrayList<>();
        this.postHooks = new ArrayList<>();
        this.sessionLifecycleHooks = new ArrayList<>();
        scanHooks(serverInstance.getClass(), tools);
    }

    /** Set the SessionLifecycle manager for session lifecycle hooks. */
    public void setSessionLifecycle(SessionLifecycle sessionLifecycle) {
        this.sessionLifecycle = sessionLifecycle;
        // Register as listener for session lifecycle events
        for (SessionLifecycleMethod hook : sessionLifecycleHooks) {
            sessionLifecycle.addListener(new SessionLifecycleAdapter(hook));
        }
    }

    private void scanHooks(Class<?> clazz, List<ToolMeta> tools) {
        for (Method method : clazz.getDeclaredMethods()) {
            // Security: Only allow hooks from the server's class hierarchy
            if (!isValidHookMethod(method)) {
                log.warn("Skipping hook method due to validation failure: {}", method.getName());
                continue;
            }

            if (method.isAnnotationPresent(McpPreHook.class)) {
                McpPreHook ann = method.getAnnotation(McpPreHook.class);
                String toolName = ann.toolName().isEmpty() ? inferToolName(method) : ann.toolName();
                preHooks.add(new HookMethod(method, toolName, ann.order(), true));
            }
            if (method.isAnnotationPresent(McpPostHook.class)) {
                McpPostHook ann = method.getAnnotation(McpPostHook.class);
                String toolName = ann.toolName().isEmpty() ? inferToolName(method) : ann.toolName();
                postHooks.add(new HookMethod(method, toolName, ann.order(), false));
            }
            if (method.isAnnotationPresent(McpSessionLifecycle.class)) {
                McpSessionLifecycle ann = method.getAnnotation(McpSessionLifecycle.class);
                sessionLifecycleHooks.add(new SessionLifecycleMethod(method, ann.value(), 0));
            }
        }

        preHooks.sort(Comparator.comparingInt(HookMethod::order));
        postHooks.sort(Comparator.comparingInt(HookMethod::order));
        sessionLifecycleHooks.sort(Comparator.comparingInt(SessionLifecycleMethod::order));

        log.info("Registered {} pre-hooks, {} post-hooks, {} session lifecycle hooks",
            preHooks.size(), postHooks.size(), sessionLifecycleHooks.size());
    }

    /** Validate hook method is safe - same classloader and safe parameter types. */
    private boolean isValidHookMethod(Method method) {
        ClassLoader methodCl = method.getDeclaringClass().getClassLoader();
        if (!isSameOrChildClassLoader(methodCl, serverClassLoader)) {
            log.warn("Hook method {} from different classloader", method.getName());
            return false;
        }
        for (Class<?> paramType : method.getParameterTypes()) {
            if (paramType == ClassLoader.class ||
                paramType == java.lang.reflect.Field.class ||
                paramType == java.lang.reflect.Constructor.class) {
                log.warn("Hook method {} has dangerous param type {}", method.getName(), paramType.getName());
                return false;
            }
        }
        return true;
    }

    /** Check if candidate classloader is same or child of parent. */
    private boolean isSameOrChildClassLoader(ClassLoader candidate, ClassLoader parent) {
        if (candidate == null) return parent == null;
        if (candidate.equals(parent)) return true;
        for (ClassLoader cl = candidate.getParent(); cl != null; cl = cl.getParent()) {
            if (cl.equals(parent)) return true;
        }
        return false;
    }

    private String inferToolName(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("pre")) {
            return methodName.substring(3);
        } else if (methodName.startsWith("post")) {
            return methodName.substring(4);
        }
        return methodName;
    }

    /**
     * Execute pre-hooks for the given tool.
     *
     * @param toolName The name of the tool being invoked
     * @param arguments The arguments passed to the tool
     */
    public void executePreHooks(String toolName, Map<String, Object> arguments) {
        for (HookMethod hook : preHooks) {
            if (hook.toolName.equals(toolName) || hook.toolName.equals("*")) {
                invokeHook(hook, arguments);
            }
        }
    }

    /**
     * Execute post-hooks for the given tool.
     *
     * @param toolName The name of the tool that was invoked
     * @param arguments The arguments passed to the tool
     * @param result The result returned by the tool
     */
    public void executePostHooks(String toolName, Map<String, Object> arguments, Object result) {
        for (HookMethod hook : postHooks) {
            if (hook.toolName.equals(toolName) || hook.toolName.equals("*")) {
                invokeHook(hook, arguments, result);
            }
        }
    }

    /**
     * Configuration for hook failure behavior.
     */
    public enum HookFailureMode {
        /** Log errors but continue execution (default) */
        WARN,
        /** Throw exceptions and halt execution */
        STRICT,
        /** Silently ignore hook failures */
        SILENT
    }

    private HookFailureMode failureMode = HookFailureMode.WARN;

    /**
     * Set the failure mode for hook execution.
     */
    public void setFailureMode(HookFailureMode mode) {
        this.failureMode = mode;
    }

    private void invokeHook(HookMethod hook, Object... args) {
        try {
            // Only set accessible if method is not public
            // This reduces reflection overhead and security risks
            if (!Modifier.isPublic(hook.method.getModifiers())) {
                hook.method.setAccessible(true);
            }

            Class<?>[] paramTypes = hook.method.getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length && i < args.length; i++) {
                if (args[i] != null) {
                    if (paramTypes[i].isAssignableFrom(args[i].getClass())) {
                        params[i] = args[i];
                    } else if (paramTypes[i] == Map.class && args[i] instanceof Map) {
                        params[i] = args[i];
                    } else if (paramTypes[i] == Object.class) {
                        params[i] = args[i];
                    }
                    // Skip parameters that don't match - they will remain null
                }
            }

            hook.method.invoke(serverInstance, params);
        } catch (Exception e) {
            switch (failureMode) {
                case STRICT:
                    throw new RuntimeException("Hook execution failed: " + hook.method.getName(), e);
                case WARN:
                    log.error("Hook execution failed: {} - {}: {}",
                        hook.method.getName(), e.getClass().getSimpleName(), e.getMessage());
                    // Log stack trace at debug level
                    log.debug("Hook execution failure details", e);
                    break;
                case SILENT:
                    // Silently ignore
                    break;
            }
        }
    }

    private record HookMethod(Method method, String toolName, int order, boolean isPre) {}

    /** Record for session lifecycle hook methods. */
    private record SessionLifecycleMethod(Method method, McpSessionLifecycle.Event event, int order) {}

    /** Adapter to convert session lifecycle hooks to SessionLifecycleListener. */
    private class SessionLifecycleAdapter implements SessionLifecycle.SessionLifecycleListener {
        private final SessionLifecycleMethod hook;

        SessionLifecycleAdapter(SessionLifecycleMethod hook) {
            this.hook = hook;
        }

        @Override
        public void onSessionBootstrap(String sessionId, SessionLifecycle.BootstrapConfig config) {
            if (hook.event == McpSessionLifecycle.Event.BOOTSTRAP) {
                invokeHookMethod(sessionId, config);
            }
        }

        @Override
        public void onSessionStart(String sessionId) {
            if (hook.event == McpSessionLifecycle.Event.START) {
                invokeHookMethod(sessionId);
            }
        }

        @Override
        public void onSessionExpiring(String sessionId, SessionLifecycle.ExpiryReason reason) {
            if (hook.event == McpSessionLifecycle.Event.EXPIRING) {
                invokeHookMethod(sessionId, reason);
            }
        }

        @Override
        public void onSessionEnd(String sessionId, SessionLifecycle.EndReason reason) {
            if (hook.event == McpSessionLifecycle.Event.END) {
                invokeHookMethod(sessionId, reason);
            }
        }

        private void invokeHookMethod(Object... args) {
            try {
                if (!Modifier.isPublic(hook.method.getModifiers())) {
                    hook.method.setAccessible(true);
                }
                hook.method.invoke(serverInstance, args);
            } catch (Exception e) {
                log.error("Session lifecycle hook failed: {} - {}",
                    hook.method.getName(), e.getMessage(), e);
            }
        }
    }
}
