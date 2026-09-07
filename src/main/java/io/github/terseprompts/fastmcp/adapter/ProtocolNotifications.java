package io.github.terseprompts.fastmcp.adapter;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.Locale;

/**
 * Pure helpers for building MCP protocol notification records.
 *
 * Extracted as static functions so the mapping logic (string level -&gt;
 * {@link McpSchema.LoggingLevel}, notification record construction) is unit
 * testable without needing a live {@link io.modelcontextprotocol.server.McpAsyncServerExchange}.
 */
public final class ProtocolNotifications {

    private ProtocolNotifications() {
    }

    /**
     * Map a string log level ("debug", "info", "warning", "error", ...) to the
     * MCP {@link McpSchema.LoggingLevel}. Unknown or null levels default to INFO.
     */
    public static McpSchema.LoggingLevel mapLevel(String level) {
        if (level == null) {
            return McpSchema.LoggingLevel.INFO;
        }
        return switch (level.toLowerCase(Locale.ROOT)) {
            case "debug" -> McpSchema.LoggingLevel.DEBUG;
            case "info" -> McpSchema.LoggingLevel.INFO;
            case "notice" -> McpSchema.LoggingLevel.NOTICE;
            case "warning" -> McpSchema.LoggingLevel.WARNING;
            case "error" -> McpSchema.LoggingLevel.ERROR;
            case "critical" -> McpSchema.LoggingLevel.CRITICAL;
            case "alert" -> McpSchema.LoggingLevel.ALERT;
            case "emergency" -> McpSchema.LoggingLevel.EMERGENCY;
            default -> McpSchema.LoggingLevel.INFO;
        };
    }

    /**
     * Build a logging message notification using the "fastmcp" logger.
     */
    public static McpSchema.LoggingMessageNotification buildLogNotification(String level, String message) {
        return McpSchema.LoggingMessageNotification.builder(mapLevel(level), message)
                .logger("fastmcp")
                .data(message)
                .build();
    }

    /**
     * Build a progress notification. A null {@code total} or {@code message} is
     * omitted from the record (both are optional in the protocol).
     */
    public static McpSchema.ProgressNotification buildProgressNotification(
            Object progressToken, double progress, Double total, String message) {
        McpSchema.ProgressNotification.Builder builder =
                McpSchema.ProgressNotification.builder(progressToken, progress);
        if (total != null) {
            builder.total(total);
        }
        if (message != null) {
            builder.message(message);
        }
        return builder.build();
    }
}