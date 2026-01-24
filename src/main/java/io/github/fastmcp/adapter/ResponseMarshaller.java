package io.github.fastmcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

import io.github.fastmcp.exception.FastMcpException;
import io.modelcontextprotocol.sdk.CallToolResult;
import io.modelcontextprotocol.sdk.TextContent;

// Lightweight response marshalling to text payloads expected by MCP client
public class ResponseMarshaller {
    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public CallToolResult marshal(Object value) {
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

        return CallToolResult.builder()
            .content(List.of(new TextContent(text)))
            .isError(false)
            .build();
    }

    private CallToolResult emptyResult() {
        return CallToolResult.builder()
            .content(List.of())
            .isError(false)
            .build();
    }
}
