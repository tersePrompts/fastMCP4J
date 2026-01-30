package com.ultrathink.fastmcp.agent.transform;

import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * SPI for transforming tool responses.
 * <p>
 * Implementations can modify responses based on URL patterns,
 * content types, or custom logic.
 */
public interface ResponseTransformer {

    /**
     * Transform a tool call result.
     *
     * @param toolName The tool that was called
     * @param arguments The tool arguments
     * @param result The original result
     * @param context Additional context (tenant, session, etc.)
     * @return The transformed result
     */
    McpSchema.CallToolResult transform(
        String toolName,
        Map<String, Object> arguments,
        McpSchema.CallToolResult result,
        TransformContext context
    );

    /**
     * Check if this transformer should apply to the given tool.
     *
     * @param toolName The tool name
     * @param context The transform context
     * @return true if this transformer should be applied
     */
    boolean appliesTo(String toolName, TransformContext context);

    /**
     * Get the transformer order (lower values execute first).
     */
    default int getOrder() {
        return 100;
    }

    /**
     * Context for transformation.
     */
    record TransformContext(
        String tenantId,
        String sessionId,
        String userId,
        Map<String, Object> metadata
    ) {
        public static TransformContext empty() {
            return new TransformContext(null, null, null, Map.of());
        }
    }
}
