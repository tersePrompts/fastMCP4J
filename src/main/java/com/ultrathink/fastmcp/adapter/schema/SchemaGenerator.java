package com.ultrathink.fastmcp.adapter.schema;

import com.ultrathink.fastmcp.exception.FastMcpException;
import com.ultrathink.fastmcp.annotations.McpParam;
import com.ultrathink.fastmcp.context.McpContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.*;
import java.util.*;

import static java.util.Objects.requireNonNull;

/** Simple schema generator converting Java method signatures to JSON-like schemas. */
public class SchemaGenerator {
    private final SchemaCache cache = new SchemaCache();

    public Map<String, Object> generate(Method method) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");

        Map<String, Object> props = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Parameter p : method.getParameters()) {
            // Skip @McpContext annotated parameters - they're framework-injected, not client parameters
            if (p.isAnnotationPresent(McpContext.class)) {
                continue;
            }

            String name = getParamName(p);
            Map<String, Object> pSchema = generateTypeSchema(p.getParameterizedType());
            
            // Create a fresh copy for this parameter
            pSchema = new HashMap<>(pSchema);
            
            // Enhance with @McpParam annotations
            enhanceSchemaWithMcpParam(pSchema, p);
            
            props.put(name, pSchema);
            if (!isOptional(p) && !isParamOptional(p)) {
                required.add(name);
            }
        }

        schema.put("properties", props);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private Map<String, Object> generateTypeSchema(Type type) {
        if (cache.has(type)) return cache.get(type);

        Map<String, Object> result;
        if (type instanceof Class<?> clazz) {
            result = generateClassSchema(clazz);
        } else if (type instanceof ParameterizedType pt) {
            result = generateGenericSchema(pt);
        } else {
            throw new FastMcpException("Unsupported type: " + type);
        }

        cache.put(type, result);
        return result;
    }

    private Map<String, Object> generateClassSchema(Class<?> clazz) {
        if (clazz == String.class) return new HashMap<>(Map.of("type", "string"));
        if (clazz == int.class || clazz == Integer.class) return new HashMap<>(Map.of("type", "integer"));
        if (clazz == long.class || clazz == Long.class) return new HashMap<>(Map.of("type", "integer", "format", "int64"));
        if (clazz == double.class || clazz == Double.class) return new HashMap<>(Map.of("type", "number"));
        if (clazz == boolean.class || clazz == Boolean.class) return new HashMap<>(Map.of("type", "boolean"));
        if (clazz.isEnum()) return enumSchema(clazz);
        // For Object.class, return empty schema to allow any type
        if (clazz == Object.class) return new HashMap<>();
        if (!clazz.isPrimitive()) return pojoSchema(clazz);
        throw new FastMcpException("Unsupported class type: " + clazz);
    }

    private Map<String, Object> pojoSchema(Class<?> clazz) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (java.lang.reflect.Modifier.isStatic(mods)) continue;
            if (field.isAnnotationPresent(JsonProperty.class)) {
                JsonProperty jp = field.getAnnotation(JsonProperty.class);
                String name = jp.value().isEmpty() ? field.getName() : jp.value();
                properties.put(name, generateTypeSchema(field.getGenericType()));
                continue;
            }
            if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnore.class)) {
                continue;
            }
            properties.put(field.getName(), generateTypeSchema(field.getGenericType()));
        }
        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> generateGenericSchema(ParameterizedType pt) {
        Type raw = pt.getRawType();
        Type[] args = pt.getActualTypeArguments();
        if (raw == List.class || raw == java.util.List.class) {
            Map<String, Object> itemSchema = generateTypeSchema(args[0]);
            return new HashMap<>(Map.of("type", "array", "items", itemSchema));
        }
        if (raw == Map.class) {
            Map<String, Object> valueSchema = generateTypeSchema(args[1]);
            return new HashMap<>(Map.of("type", "object", "additionalProperties", valueSchema));
        }
        // Fallback for other generics
        throw new FastMcpException("Unsupported generic type: " + pt);
    }

    private Map<String, Object> enumSchema(Class<?> clazz) {
        Object[] constants = clazz.getEnumConstants();
        String[] values = Arrays.stream(constants).map(Object::toString).toArray(String[]::new);
        return new HashMap<>(Map.of("type", "string", "enum", values));
    }

    private String getParamName(Parameter p) {
        JsonProperty ann = p.getAnnotation(JsonProperty.class);
        if (ann != null && !ann.value().isEmpty()) return ann.value();
        if (p.isNamePresent()) return p.getName();
        throw new FastMcpException("Cannot determine parameter name. Use @JsonProperty or compile with -parameters");
    }

    private boolean isOptional(Parameter p) {
        Type t = p.getParameterizedType();
        if (t instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            if (raw == java.util.Optional.class) return true;
        }
        return false;
    }

    private boolean isParamOptional(Parameter p) {
        McpParam ann = p.getAnnotation(McpParam.class);
        return ann != null && !ann.required();
    }

    private void enhanceSchemaWithMcpParam(Map<String, Object> schema, Parameter param) {
        McpParam ann = param.getAnnotation(McpParam.class);
        if (ann == null) return;

        // Build enhanced description with examples, constraints, and hints
        StringBuilder description = new StringBuilder();

        // Main description
        if (!ann.description().isEmpty()) {
            description.append(ann.description());
        }

        // Examples
        if (ann.examples().length > 0) {
            schema.put("examples", Arrays.asList(ann.examples()));
            description.append("\n\nExamples: ");
            description.append(String.join(", ", ann.examples()));
        }

        // Constraints (included in description, not as separate field for JSON Schema compliance)
        if (!ann.constraints().isEmpty()) {
            description.append("\n\nConstraints: ").append(ann.constraints());
        }

        // Hints for LLM (included in description, not as separate field for JSON Schema compliance)
        if (!ann.hints().isEmpty()) {
            description.append("\n\nHints: ").append(ann.hints());
        }

        // Set the enhanced description (includes examples, constraints, hints for LLM visibility)
        if (description.length() > 0) {
            schema.put("description", description.toString());
        }

        // Add default value if provided
        if (!ann.defaultValue().isEmpty()) {
            schema.put("default", ann.defaultValue());
        }

        // Note: required status is handled at root schema level, not property level (JSON Schema 2020-12)
    }
    }
}
