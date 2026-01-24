# CHUNK 4: Schema Generator

**Dependencies**: CHUNK 2 (Model), CHUNK 0 (Exception)

**Files**:
- `src/main/java/io/github/fastmcp/schema/SchemaGenerator.java`
- `src/main/java/io/github/fastmcp/schema/SchemaCache.java`
- `src/test/java/io/github/fastmcp/schema/SchemaGeneratorTest.java`

---

## Implementation

### SchemaCache.java
```java
package io.github.fastmcp.schema;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SchemaCache {
    private final Map<Type, Map<String, Object>> cache = new ConcurrentHashMap<>();

    public boolean has(Type type) {
        return cache.containsKey(type);
    }

    public Map<String, Object> get(Type type) {
        return cache.get(type);
    }

    public void put(Type type, Map<String, Object> schema) {
        cache.put(type, schema);
    }
}
```

### SchemaGenerator.java
```java
package io.github.fastmcp.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.fastmcp.exception.FastMcpException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
public class SchemaGenerator {
    private final SchemaCache cache = new SchemaCache();

    public Map<String, Object> generate(Method method) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> props = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Parameter p : method.getParameters()) {
            String name = getParamName(p);
            props.put(name, typeToSchema(p.getParameterizedType()));
            required.add(name);
        }

        schema.put("properties", props);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        return schema;
    }

    private Map<String, Object> typeToSchema(Type type) {
        if (cache.has(type)) {
            return cache.get(type);
        }

        Map<String, Object> schema = generateSchema(type);
        cache.put(type, schema);
        return schema;
    }

    private Map<String, Object> generateSchema(Type type) {
        if (type == int.class || type == Integer.class) {
            return Map.of("type", "integer");
        }
        if (type == long.class || type == Long.class) {
            return Map.of("type", "integer", "format", "int64");
        }
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            return Map.of("type", "number");
        }
        if (type == boolean.class || type == Boolean.class) {
            return Map.of("type", "boolean");
        }
        if (type == String.class) {
            return Map.of("type", "string");
        }

        if (type instanceof Class<?> clazz) {
            if (clazz.isEnum()) {
                return enumSchema(clazz);
            }
        }

        if (type instanceof ParameterizedType pt) {
            return genericSchema(pt);
        }

        throw new FastMcpException("Unsupported type: " + type);
    }

    private Map<String, Object> enumSchema(Class<?> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        List<String> values = Arrays.stream(constants)
            .map(Object::toString)
            .toList();

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        schema.put("enum", values);
        return schema;
    }

    private Map<String, Object> genericSchema(ParameterizedType pt) {
        Type rawType = pt.getRawType();

        if (rawType == List.class || rawType == java.util.Collection.class) {
            Type itemType = pt.getActualTypeArguments()[0];
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "array");
            schema.put("items", typeToSchema(itemType));
            return schema;
        }

        if (rawType == Map.class) {
            Type valueType = pt.getActualTypeArguments()[1];
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "object");
            schema.put("additionalProperties", typeToSchema(valueType));
            return schema;
        }

        throw new FastMcpException("Unsupported generic type: " + pt);
    }

    private String getParamName(Parameter p) {
        JsonProperty ann = p.getAnnotation(JsonProperty.class);
        if (ann != null && !ann.value().isEmpty()) {
            return ann.value();
        }

        if (p.isNamePresent()) {
            return p.getName();
        }

        throw new FastMcpException(
            "Cannot determine parameter name. Use @JsonProperty or compile with -parameters"
        );
    }
}
```

---

## Tests

### SchemaGeneratorTest.java
```java
package io.github.fastmcp.schema;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SchemaGeneratorTest {

    private final SchemaGenerator generator = new SchemaGenerator();

    @Test
    void testPrimitiveTypes() throws Exception {
        class TestClass {
            public void method(int a, String b, boolean c) {}
        }

        Method method = TestClass.class.getMethod("method", int.class, String.class, boolean.class);
        Map<String, Object> schema = generator.generate(method);

        assertEquals("object", schema.get("type"));
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        assertEquals("integer", ((Map<?, ?>) props.get("a")).get("type"));
        assertEquals("string", ((Map<?, ?>) props.get("b")).get("type"));
        assertEquals("boolean", ((Map<?, ?>) props.get("c")).get("type"));
    }

    @Test
    void testListType() throws Exception {
        class TestClass {
            public void method(List<String> items) {}
        }

        Method method = TestClass.class.getMethod("method", List.class);
        Map<String, Object> schema = generator.generate(method);

        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        Map<String, Object> itemsSchema = (Map<String, Object>) props.get("items");

        assertEquals("array", itemsSchema.get("type"));
        Map<String, Object> itemType = (Map<String, Object>) itemsSchema.get("items");
        assertEquals("string", itemType.get("type"));
    }

    @Test
    void testCaching() throws Exception {
        class TestClass {
            public void method1(String a) {}
            public void method2(String b) {}
        }

        Method m1 = TestClass.class.getMethod("method1", String.class);
        Method m2 = TestClass.class.getMethod("method2", String.class);

        generator.generate(m1);
        assertTrue(generator.cache.has(String.class));

        generator.generate(m2);
    }
}
```

---

## Verification
- [ ] Generator handles all primitive types
- [ ] List and Map generics work
- [ ] Enum types generate enum schema
- [ ] Caching works correctly
- [ ] Clear error for unsupported types
