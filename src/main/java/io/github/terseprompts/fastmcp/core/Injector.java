package io.github.terseprompts.fastmcp.core;

import io.github.terseprompts.fastmcp.annotations.McpInject;
import io.github.terseprompts.fastmcp.annotations.McpOnShutdown;
import io.github.terseprompts.fastmcp.annotations.McpOnStartup;
import io.github.terseprompts.fastmcp.context.Context;
import io.github.terseprompts.fastmcp.context.ContextImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Lightweight dependency-injection and lifecycle helper used by the FastMCP
 * builder. Kept as static, testable methods so behavior can be verified without
 * spinning up a full server.
 */
public final class Injector {

    private Injector() {
    }

    /**
     * Populate all {@code @McpInject} fields on {@code target} from the registry.
     * Static and final fields are skipped; unmatched fields are left untouched.
     */
    public static void injectFields(Object target, InjectionRegistry registry) {
        if (target == null || registry == null) {
            return;
        }
        for (Class<?> clazz = target.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(McpInject.class)) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                Object value = registry.resolve(field.getName(), field.getType());
                if (value == null) {
                    continue;
                }
                try {
                    if (!field.canAccess(target)) {
                        field.setAccessible(true);
                    }
                    field.set(target, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to inject field: " + field.getName(), e);
                }
            }
        }
    }

    /**
     * Invoke all {@code @McpOnStartup} methods on {@code target}. Methods taking a
     * single {@code Context}-typed parameter receive the current ThreadLocal context
     * when available, otherwise they are invoked with no arguments.
     */
    public static void invokeStartup(Object target) {
        if (target == null) {
            return;
        }
        for (Class<?> clazz = target.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(McpOnStartup.class)) {
                    continue;
                }
                invokeAvailable(target, method);
            }
        }
    }

    /**
     * Invoke all {@code @McpOnShutdown} methods (zero-arg) on {@code target}.
     */
    public static void invokeShutdown(Object target) {
        if (target == null) {
            return;
        }
        for (Class<?> clazz = target.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(McpOnShutdown.class)) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 0) {
                    continue;
                }
                invoke(target, method, new Object[0]);
            }
        }
    }

    private static void invokeAvailable(Object target, Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 0) {
            invoke(target, method, new Object[0]);
            return;
        }
        if (params.length == 1 && Context.class.isAssignableFrom(params[0])) {
            Context ctx = ContextImpl.getCurrentContextOrNull();
            invoke(target, method, new Object[]{ctx});
            return;
        }
        // Unsupported signature: skip safely.
    }

    private static void invoke(Object target, Method method, Object[] args) {
        try {
            if (!method.canAccess(target)) {
                method.setAccessible(true);
            }
            method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke lifecycle method: " + method.getName(), e);
        }
    }
}