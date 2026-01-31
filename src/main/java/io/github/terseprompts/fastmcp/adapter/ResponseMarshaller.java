package io.github.terseprompts.fastmcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

import io.github.terseprompts.fastmcp.exception.FastMcpException;
import io.github.terseprompts.fastmcp.json.ObjectMapperFactory;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Converts method return values to MCP CallToolResult with security hardening.
 */
public class ResponseMarshaller {

    private final ObjectMapper mapper;

    public ResponseMarshaller() {
        this.mapper = ObjectMapperFactory.getShared();
    }

    public ResponseMarshaller(ObjectMapper customMapper) {
        this.mapper = customMapper != null ? customMapper : ObjectMapperFactory.getShared();
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
