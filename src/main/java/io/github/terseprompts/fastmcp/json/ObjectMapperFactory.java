package io.github.terseprompts.fastmcp.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.StreamReadConstraints;

/**
 * Centralized factory for creating secure Jackson ObjectMappers.
 * Eliminates duplicate ObjectMapper creation code across the codebase.
 */
public final class ObjectMapperFactory {

    private static final ObjectMapper SHARED_INSTANCE = createSecureObjectMapper();

    private ObjectMapperFactory() {}

    /**
     * Get the shared secure ObjectMapper instance.
     * This is thread-safe and can be safely used across the application.
     */
    public static ObjectMapper getShared() {
        return SHARED_INSTANCE;
    }

    /**
     * Create a new secure ObjectMapper with the same security settings.
     * Use this when a new instance is needed instead of the shared one.
     */
    public static ObjectMapper createNew() {
        return createSecureObjectMapper();
    }

    /**
     * Create a Jackson ObjectMapper with security constraints.
     * This is the central definition - all ObjectMappers should use these settings.
     */
    private static ObjectMapper createSecureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Set stream read constraints to prevent DoS via large payloads
        mapper.getFactory().setStreamReadConstraints(
            StreamReadConstraints.builder()
                .maxDocumentLength(10_000_000)  // 10MB max document
                .maxStringLength(1_000_000)     // 1MB max string
                .maxNameLength(100_000)         // 100KB max name
                .build()
        );

        return mapper;
    }

    /**
     * Create a custom ObjectMapper with the same security constraints.
     * @param config Custom configuration function applied after security settings
     */
    public static ObjectMapper createSecure(java.util.function.Consumer<ObjectMapper> config) {
        ObjectMapper mapper = createSecureObjectMapper();
        if (config != null) {
            config.accept(mapper);
        }
        return mapper;
    }
}
