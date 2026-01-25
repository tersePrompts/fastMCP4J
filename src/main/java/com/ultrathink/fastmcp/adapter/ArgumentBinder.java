package com.ultrathink.fastmcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ultrathink.fastmcp.context.Context;
import com.ultrathink.fastmcp.context.ContextImpl;
import com.ultrathink.fastmcp.context.McpContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;
import com.fasterxml.jackson.databind.MapperFeature;

/**
 * Converts JSON request arguments to typed method parameters.
 * Uses Jackson for type conversion. Validates required parameters (non-null for primitives).
 * Resolves parameter names from @JsonProperty or -parameters compiler flag.
 * Supports @McpContext annotation for injecting Context objects.
 */
public class ArgumentBinder {
    private final ObjectMapper mapper = new ObjectMapper();

    public Object[] bind(Method method, Map<String, Object> args) {
        Parameter[] params = method.getParameters();
        Object[] bound = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            // Check if parameter is a Context injection point
            if (params[i].isAnnotationPresent(McpContext.class) || 
                Context.class.isAssignableFrom(params[i].getType())) {
                
                // Inject current context from ThreadLocal
                bound[i] = ContextImpl.getCurrentContext();
                continue;
            }
            
            String name = getParamName(params[i]);
            Object raw = args.get(name);
            
            // Validate that required parameters are not null
            if (raw == null && !isNullAllowed(params[i].getType())) {
                throw new IllegalArgumentException(
                    "Missing required parameter: " + name
                );
            }
            
            bound[i] = mapper.convertValue(raw, params[i].getType());
        }

        return bound;
    }

    private boolean isNullAllowed(Class<?> type) {
        // Primitive types cannot be null
        // Wrapper types and other reference types can be null
        return !type.isPrimitive();
    }

    private String getParamName(Parameter p) {
        JsonProperty ann = p.getAnnotation(JsonProperty.class);
        if (ann != null && !ann.value().isEmpty()) {
            return ann.value();
        }
        if (p.isNamePresent()) {
            return p.getName();
        }
        throw new IllegalArgumentException(
            "Cannot determine parameter name. Use @JsonProperty or compile with -parameters"
        );
    }
}
