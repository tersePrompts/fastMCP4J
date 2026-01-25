package com.ultrathink.fastmcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ultrathink.fastmcp.exception.FastMcpException;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;

/**
 * Converts method return values to MCP GetPromptResult.
 * Handles different return types: String, PromptMessage, List<PromptMessage>, and objects.
 * String → single user message with text content. Objects → JSON serialized in user message.
 */
public class PromptResponseMarshaller {
    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public McpSchema.GetPromptResult marshal(Object value) {
        if (value == null) return emptyResult();

        // Handle different return types
        if (value instanceof String s) {
            return stringResult(s);
        } else if (value instanceof McpSchema.PromptMessage message) {
            return messageResult(message);
        } else if (value instanceof List<?> list) {
            return handleList(list);
        } else {
            // Serialize objects to JSON in a user message
            try {
                String json = mapper.writeValueAsString(value);
                return stringResult(json);
            } catch (Exception e) {
                throw new FastMcpException("Failed to marshal prompt response", e);
            }
        }
    }

    private McpSchema.GetPromptResult stringResult(String text) {
        return new McpSchema.GetPromptResult(
            "Generated prompt",
            List.of(new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(text)
            ))
        );
    }

    private McpSchema.GetPromptResult messageResult(McpSchema.PromptMessage message) {
        return new McpSchema.GetPromptResult(
            "Generated prompt",
            List.of(message)
        );
    }

    @SuppressWarnings("unchecked")
    private McpSchema.GetPromptResult handleList(List<?> list) {
        if (list.isEmpty()) {
            return emptyResult();
        }

        Object first = list.get(0);
        
        // If list of PromptMessage, use directly
        if (first instanceof McpSchema.PromptMessage) {
            return new McpSchema.GetPromptResult(
                "Generated prompt",
                (List<McpSchema.PromptMessage>) list
            );
        }
        
        // If list of strings, create user messages for each
        if (first instanceof String) {
            List<McpSchema.PromptMessage> messages = ((List<String>) list).stream()
                .map(text -> new McpSchema.PromptMessage(
                    McpSchema.Role.USER,
                    new McpSchema.TextContent(text)
                ))
                .toList();
            
            return new McpSchema.GetPromptResult(
                "Generated prompt",
                messages
            );
        }
        
        // Otherwise serialize the entire list as JSON in a single message
        try {
            String json = mapper.writeValueAsString(list);
            return stringResult(json);
        } catch (Exception e) {
            throw new FastMcpException("Failed to marshal prompt list response", e);
        }
    }

    private McpSchema.GetPromptResult emptyResult() {
        return new McpSchema.GetPromptResult(
            "Empty prompt",
            List.of()
        );
    }
}