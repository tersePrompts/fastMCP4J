package com.ultrathink.fastmcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.StreamReadConstraints;
import java.util.List;

import com.ultrathink.fastmcp.exception.FastMcpException;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Converts method return values to MCP CallToolResult with security hardening.
 *
 * Security enhancements:
 * - StreamReadConstraints on ObjectMapper
 * - NON_NULL serialization to reduce payload size
 */
public class ResponseMarshaller {

    private final ObjectMapper mapper;

    public ResponseMarshaller() {
        this.mapper = createSecureObjectMapper();
    }

    public ResponseMarshaller(ObjectMapper customMapper) {
        this.mapper = customMapper != null ? customMapper : createSecureObjectMapper();
    }

    /**
     * Create a Jackson ObjectMapper with security constraints.
     */
    private static ObjectMapper createSecureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Set serialization inclusion to reduce payload size
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Set stream read constraints for consistency
        mapper.getFactory().setStreamReadConstraints(
            StreamReadConstraints.builder()
                .maxDocumentLength(10_000_000)
                .maxStringLength(1_000_000)
                .maxNameLength(100_000)
                .build()
        );

        return mapper;
    }

    public McpSchema.CallToolResult marshal(Object value) {
        if (value == null) return emptyResult();

        String text;
        if (value instanceof String s) {
            text = s;
        } else if (value instanceof Number n) {
            text = n.toString();
        } else if (value instanceof Boolean b) {
            text = b.toString();
        } else {
            try {
                text = mapper.writeValueAsString(value);
            } catch (Exception e) {
                throw new FastMcpException("Failed to marshal response", e);
            }
        }

        return McpSchema.CallToolResult.builder()
            .content(List.of(new McpSchema.TextContent(text)))
            .isError(false)
            .build();
    }

    private McpSchema.CallToolResult emptyResult() {
        return McpSchema.CallToolResult.builder()
            .content(List.of())
            .isError(false)
            .build();
    }
}
