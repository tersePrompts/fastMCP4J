package io.github.terseprompts.fastmcp.adapter.schema;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Simple type -> JSON schema cache */
public class SchemaCache {
    private final Map<Type, Map<String, Object>> cache = new ConcurrentHashMap<>();

    public boolean has(Type t) { return cache.containsKey(t); }
    public Map<String, Object> get(Type t) { return cache.get(t); }
    public void put(Type t, Map<String, Object> schema) { cache.put(t, schema); }
}
