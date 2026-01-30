package com.ultrathink.fastmcp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable telemetry for an MCP server.
 * <p>
 * When present on a server class, enables metrics and tracing collection.
 * Telemetry data is exported to the configured destination (console by default).
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @McpServer
 * @McpTelemetry(
 *     enabled = true,
 *     exportConsole = true,
 *     exportOtlp = false,
 *     sampleRate = 1.0
 * )
 * public class MyServer {
 *     @McpTool
 *     public String myTool(String input) {
 *         return "result";
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTelemetry {

    /**
     * Enable or disable telemetry collection.
     */
    boolean enabled() default true;

    /**
     * Export telemetry to console.
     */
    boolean exportConsole() default true;

    /**
     * Export telemetry to OTLP endpoint.
     * Requires io.opentelemetry:opentelemetry-exporter-otlp on classpath.
     */
    boolean exportOtlp() default false;

    /**
     * OTLP endpoint URL (e.g., http://localhost:4317).
     */
    String otlpEndpoint() default "http://localhost:4317";

    /**
     * Sample rate for traces (0.0 to 1.0).
     * 1.0 means all requests are traced, 0.1 means 10% are traced.
     */
    double sampleRate() default 1.0;

    /**
     * Include detailed tool arguments in traces (may expose sensitive data).
     */
    boolean includeArguments() default false;

    /**
     * Export interval for metrics in milliseconds.
     */
    long metricExportIntervalMs() default 60_000;
}
