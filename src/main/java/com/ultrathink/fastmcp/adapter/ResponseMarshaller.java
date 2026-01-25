package com.ultrathink.fastmcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

import com.ultrathink.fastmcp.exception.FastMcpException;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Converts method return values to MCP CallToolResult.
 * Primitives/String → text content. Objects → JSON serialized. null → empty result.
 * <p>
 * Note: We don't serialize the result to JSON here. The McpSchema.TextContent object
 * will be serialized by the MCP SDK's JacksonMcpJsonMapper, which handles the proper
 * format for TextContent (with 'type' and 'text' fields).
 */
public class ResponseMarshaller {

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
            // For complex objects, convert to JSON string
            ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
