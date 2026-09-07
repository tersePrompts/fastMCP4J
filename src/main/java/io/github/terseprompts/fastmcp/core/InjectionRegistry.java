package io.github.terseprompts.fastmcp.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry that stores beans to be injected into {@code @McpInject} fields.
 * Beans may be registered by a String id (matched against the field name) and/or
 * by Class type (matched against the field's declared type).
 */
public class InjectionRegistry {

    private final Map<String, Object> byId = new HashMap<>();
    private final Map<Class<?>, Object> byType = new HashMap<>();

    /**
     * Register a bean by name.
     */
    public InjectionRegistry register(String id, Object bean) {
        if (id != null && bean != null) {
            byId.put(id, bean);
        }
        return this;
    }

    /**
     * Register a bean by type.
     */
    public InjectionRegistry register(Class<?> type, Object bean) {
        if (type != null && bean != null) {
            byType.put(type, bean);
        }
        return this;
    }

    /**
     * Remove a bean by id.
     */
    public InjectionRegistry unregister(String id) {
        if (id != null) {
            byId.remove(id);
        }
        return this;
    }

    /**
     * Resolve a bean for a field, first by the field name (bean id), then by the
     * field's declared type. Returns null when there is no match.
     */
    public Object resolve(String fieldName, Class<?> fieldType) {
        if (fieldName != null && byId.containsKey(fieldName)) {
            return byId.get(fieldName);
        }
        if (fieldType != null && byType.containsKey(fieldType)) {
            return byType.get(fieldType);
        }
        return null;
    }

    /**
     * @return true when the registry holds no beans.
     */
    public boolean isEmpty() {
        return byId.isEmpty() && byType.isEmpty();
    }
}