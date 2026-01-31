package com.ultrathink.fastmcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.ultrathink.fastmcp.context.Context;
import com.ultrathink.fastmcp.context.ContextImpl;
import com.ultrathink.fastmcp.context.McpContext;
import com.ultrathink.fastmcp.json.ObjectMapperFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Converts JSON request arguments to typed method parameters.
 * Uses Jackson for type conversion with security hardening.
 * Validates required parameters (non-null for primitives).
 * Resolves parameter names from @JsonProperty or -parameters compiler flag.
 * Supports @McpContext annotation for injecting Context objects.
 *
 * Security enhancements:
 * - StreamReadConstraints to limit input size
 * - FAIL_ON_UNKNOWN_PROPERTIES disabled for flexibility
 * - Safe defaults for deserialization
 * - Argument count and size validation to prevent DoS
 */
public class ArgumentBinder {
    // Maximum number of arguments to prevent DoS via excessive parameters
    private static final int MAX_ARGUMENT_COUNT = 50;

    // Maximum size for a single argument value (as JSON string)
    private static final int MAX_ARGUMENT_SIZE = 1_000_000; // 1MB

    private final ObjectMapper mapper;

    public ArgumentBinder() {
        this.mapper = createSecureObjectMapper();
    }

    public ArgumentBinder(ObjectMapper customMapper) {
        this.mapper = customMapper != null ? customMapper : createSecureObjectMapper();
    }

    /**
     * Create a Jackson ObjectMapper with security constraints and deserialization config.
     */
    private static ObjectMapper createSecureObjectMapper() {
        ObjectMapper mapper = ObjectMapperFactory.createNew();

        // Configure deserialization settings
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        return mapper;
    }

    public Object[] bind(Method method, Map<String, Object> args) {
        // Validate argument count to prevent DoS
        if (args != null && args.size() > MAX_ARGUMENT_COUNT) {
            throw new IllegalArgumentException(
                "Too many arguments: " + args.size() + " exceeds maximum of " + MAX_ARGUMENT_COUNT
            );
        }

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

            // Validate argument size to prevent DoS via large payloads
            if (raw != null) {
                validateArgumentSize(name, raw);
            }

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

    /**
     * Validate argument size to prevent DoS via large payloads.
     */
    private void validateArgumentSize(String name, Object value) {
        // Check string size
        if (value instanceof String s) {
            if (s.length() > MAX_ARGUMENT_SIZE) {
                throw new IllegalArgumentException(
                    "Argument '" + name + "' exceeds maximum size of " + MAX_ARGUMENT_SIZE + " characters"
                );
            }
            return;
        }

        // Check array/collection size
        if (value instanceof Object[] arr) {
            if (arr.length > MAX_ARGUMENT_COUNT) {
                throw new IllegalArgumentException(
                    "Argument '" + name + "' array exceeds maximum length of " + MAX_ARGUMENT_COUNT
                );
            }
            return;
        }

        if (value instanceof java.util.Collection<?> coll) {
            if (coll.size() > MAX_ARGUMENT_COUNT) {
                throw new IllegalArgumentException(
                    "Argument '" + name + "' collection exceeds maximum size of " + MAX_ARGUMENT_COUNT
                );
            }
            return;
        }

        // Check map size
        if (value instanceof java.util.Map<?, ?> map) {
            if (map.size() > MAX_ARGUMENT_COUNT) {
                throw new IllegalArgumentException(
                    "Argument '" + name + "' map exceeds maximum size of " + MAX_ARGUMENT_COUNT
                );
            }
            return;
        }

        // For other types, check serialized size
        try {
            byte[] serialized = mapper.writeValueAsBytes(value);
            if (serialized.length > MAX_ARGUMENT_SIZE) {
                throw new IllegalArgumentException(
                    "Argument '" + name + "' exceeds maximum serialized size of " + MAX_ARGUMENT_SIZE + " bytes"
                );
            }
        } catch (Exception e) {
            // If we can't serialize, skip the size check
            // The ObjectMapper will handle any actual serialization issues
        }
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

    /**
     * Get the configured ObjectMapper for external use if needed.
     */
    public ObjectMapper getObjectMapper() {
        return mapper;
    }
}
