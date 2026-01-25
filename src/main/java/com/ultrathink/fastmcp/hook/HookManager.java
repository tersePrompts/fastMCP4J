package com.ultrathink.fastmcp.hook;

import com.ultrathink.fastmcp.annotations.McpPreHook;
import com.ultrathink.fastmcp.annotations.McpPostHook;
import com.ultrathink.fastmcp.model.ToolMeta;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
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
 */
@Slf4j
public class HookManager {
    private final Object serverInstance;
    private final List<HookMethod> preHooks;
    private final List<HookMethod> postHooks;

    public HookManager(Object serverInstance, List<ToolMeta> tools) {
        this.serverInstance = serverInstance;
        this.preHooks = new ArrayList<>();
        this.postHooks = new ArrayList<>();
        scanHooks(serverInstance.getClass(), tools);
    }

    private void scanHooks(Class<?> clazz, List<ToolMeta> tools) {
        for (Method method : clazz.getDeclaredMethods()) {
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
        }

        preHooks.sort(Comparator.comparingInt(HookMethod::order));
        postHooks.sort(Comparator.comparingInt(HookMethod::order));

        log.info("Registered {} pre-hooks and {} post-hooks", preHooks.size(), postHooks.size());
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

    private void invokeHook(HookMethod hook, Object... args) {
        try {
            hook.method.setAccessible(true);

            Class<?>[] paramTypes = hook.method.getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length && i < args.length; i++) {
                if (paramTypes[i].isAssignableFrom(args[i].getClass())) {
                    params[i] = args[i];
                } else if (paramTypes[i] == Map.class && args[i] instanceof Map) {
                    params[i] = args[i];
                } else if (paramTypes[i] == Object.class) {
                    params[i] = args[i];
                }
            }

            hook.method.invoke(serverInstance, params);
        } catch (Exception e) {
            log.error("Hook execution failed: {}", hook.method.getName(), e);
        }
    }

    private record HookMethod(Method method, String toolName, int order, boolean isPre) {}
}
