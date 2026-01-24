package com.ultrathink.fastmcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ultrathink.fastmcp.exception.FastMcpException;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;

/**
 * Converts method return values to MCP ReadResourceResult.
 * Handles different return types: String, byte[], objects, and collections.
 * String/byte[] → direct text/blob content. Objects → JSON serialized.
 */
public class ResourceResponseMarshaller {
    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public McpSchema.ReadResourceResult marshal(Object value) {
        if (value == null) return emptyResult();

        // Handle different return types
        if (value instanceof String s) {
            return textResult(s);
        } else if (value instanceof byte[] bytes) {
            return blobResult(bytes);
        } else if (value instanceof List<?> list) {
            return handleList(list);
        } else {
            // Serialize objects to JSON
            try {
                String json = mapper.writeValueAsString(value);
                return textResult(json);
            } catch (Exception e) {
                throw new FastMcpException("Failed to marshal resource response", e);
            }
        }
    }

    private McpSchema.ReadResourceResult textResult(String text) {
        // Use BlobResourceContents with base64 encoded text for now
        return new McpSchema.ReadResourceResult(List.of(
            new McpSchema.BlobResourceContents(
                "text/plain", 
                null, 
                java.util.Base64.getEncoder().encodeToString(text.getBytes())
            )
        ));
    }

    private McpSchema.ReadResourceResult blobResult(byte[] bytes) {
        return new McpSchema.ReadResourceResult(List.of(
            (McpSchema.ResourceContents) new McpSchema.BlobResourceContents(
                "application/octet-stream", 
                null, 
                java.util.Base64.getEncoder().encodeToString(bytes)
            )
        ));
    }

    @SuppressWarnings("unchecked")
    private McpSchema.ReadResourceResult handleList(List<?> list) {
        if (list.isEmpty()) {
            return emptyResult();
        }

        Object first = list.get(0);
        
        // If list of strings, join them
        if (first instanceof String) {
            return textResult(String.join("\n", (List<String>) list));
        }
        
            // If list of byte arrays, treat as multiple blob contents
        if (first instanceof byte[]) {
            List<McpSchema.ResourceContents> blobContents = list.stream()
                .map(item -> (byte[]) item)
                .map(bytes -> (McpSchema.ResourceContents) new McpSchema.BlobResourceContents(
                    "application/octet-stream", 
                    null, 
                    java.util.Base64.getEncoder().encodeToString(bytes)
                ))
                .toList();
            
            return new McpSchema.ReadResourceResult(blobContents);
        }
        
        // Otherwise serialize the entire list as JSON
        try {
            String json = mapper.writeValueAsString(list);
            return textResult(json);
        } catch (Exception e) {
            throw new FastMcpException("Failed to marshal resource list response", e);
        }
    }

    private McpSchema.ReadResourceResult emptyResult() {
        return new McpSchema.ReadResourceResult(List.of());
    }
}