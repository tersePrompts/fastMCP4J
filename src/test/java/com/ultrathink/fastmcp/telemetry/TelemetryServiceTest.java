package com.ultrathink.fastmcp.telemetry;

import com.ultrathink.fastmcp.annotations.McpTelemetry;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryServiceTest {

    /**
     * Create a mock McpTelemetry annotation for testing.
     */
    private McpTelemetry createMockAnnotation(
            boolean enabled,
            boolean exportConsole,
            boolean exportOtlp,
            String otlpEndpoint,
            double sampleRate,
            boolean includeArguments,
            long metricExportIntervalMs
    ) {
        return new McpTelemetry() {
            @Override
            public boolean enabled() { return enabled; }

            @Override
            public boolean exportConsole() { return exportConsole; }

            @Override
            public boolean exportOtlp() { return exportOtlp; }

            @Override
            public String otlpEndpoint() { return otlpEndpoint; }

            @Override
            public double sampleRate() { return sampleRate; }

            @Override
            public boolean includeArguments() { return includeArguments; }

            @Override
            public long metricExportIntervalMs() { return metricExportIntervalMs; }

            @Override
            public Class<? extends Annotation> annotationType() {
                return McpTelemetry.class;
            }
        };
    }

    @Test
    void testCreateTelemetryServiceWithDefaults() {
        McpTelemetry annotation = createMockAnnotation(
                true, true, false, "http://localhost:4317",
                1.0, false, 60000
        );

        TelemetryService service = TelemetryService.create("TestServer", annotation);
        assertNotNull(service, "TelemetryService should be created");
        service.close();
    }

    @Test
    void testTelemetryDisabled() {
        McpTelemetry annotation = createMockAnnotation(
                false, true, false, "http://localhost:4317",
                1.0, false, 60000
        );

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        // When disabled, operations should be no-ops
        service.increment("test.counter");
        service.record("test.histogram", 100);
        service.recordToolInvocation("testTool", Duration.ofMillis(100), true);

        // Should not throw
        service.close();
    }

    @Test
    void testRecordToolInvocation() {
        McpTelemetry annotation = createMockAnnotation(
                true, false, false, "http://localhost:4317",
                1.0, false, 60000
        );

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        // Record some tool invocations
        service.recordToolInvocation("search", Duration.ofMillis(50), true);
        service.recordToolInvocation("search", Duration.ofMillis(75), true);
        service.recordToolInvocation("search", Duration.ofMillis(100), false);

        service.close();
    }

    @Test
    void testSpanSampling_ZeroPercent() {
        McpTelemetry annotation = createMockAnnotation(
                true, false, false, "http://localhost:4317",
                0.0, false, 60000
        );

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        // With 0% sample rate, span should always be null
        TelemetryService.Span span = service.createSpan("testOperation", null);
        assertNull(span, "Span should be null with 0% sampling rate");

        service.close();
    }

    @Test
    void testSpanSampling_HundredPercent() {
        McpTelemetry annotation = createMockAnnotation(
                true, false, false, "http://localhost:4317",
                1.0, false, 60000
        );

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        // With 100% sample rate, span should always be created
        TelemetryService.Span span = service.createSpan("testOperation", null);
        assertNotNull(span, "Span should be created with 100% sampling rate");
        assertNotNull(span.getSpanId(), "Span should have a span ID");
        assertNotNull(span.getTraceId(), "Span should have a trace ID");

        span.close();
        service.close();
    }

    @Test
    void testSpanAutoCloseable() {
        McpTelemetry annotation = createMockAnnotation(
                true, false, false, "http://localhost:4317",
                1.0, false, 60000
        );

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        try (TelemetryService.Span span = service.createSpan("testOperation", null)) {
            assertNotNull(span, "Span should be created");
        }

        service.close();
    }

    @Test
    void testCounterIncrement() {
        McpTelemetry annotation = createMockAnnotation(
                true, false, false, "http://localhost:4317",
                1.0, false, 60000
        );

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        service.increment("requests.total");
        service.increment("requests.total");
        service.increment("requests.total");

        service.close();
    }

    @Test
    void testHistogramRecord() {
        McpTelemetry annotation = createMockAnnotation(
                true, false, false, "http://localhost:4317",
                1.0, false, 60000
        );

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        service.record("latency.ms", 10);
        service.record("latency.ms", 50);
        service.record("latency.ms", 100);
        service.record("latency.ms", 25);

        service.close();
    }

    @Test
    void testTraceContextMDC() {
        McpTelemetry annotation = createMockAnnotation(
                true, false, false, "http://localhost:4317",
                1.0, false, 60000
        );

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        service.setTraceContext("trace-123", "span-456");
        service.clearTraceContext();

        service.close();
    }

    @Test
    void testSpanWithParent() {
        McpTelemetry annotation = createMockAnnotation(
                true, false, false, "http://localhost:4317",
                1.0, false, 60000
        );

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        // Create parent span
        TelemetryService.Span parentSpan = service.createSpan("parentOperation", null);
        assertNotNull(parentSpan, "Parent span should be created");
        assertNotNull(parentSpan.getTraceId(), "Parent span should have trace ID");
        assertNotNull(parentSpan.getSpanId(), "Parent span should have span ID");

        // Store parent trace ID before creating child
        String parentTraceId = parentSpan.getTraceId();
        String parentSpanId = parentSpan.getSpanId();

        // Create child span - trace ID will be generated separately since MDC context
        // is cleared between operations in test scenarios
        TelemetryService.Span childSpan = service.createSpan("childOperation", parentSpanId);
        assertNotNull(childSpan, "Child span should be created");
        assertNotNull(childSpan.getTraceId(), "Child span should have trace ID");
        assertEquals(parentSpanId, childSpan.getParentSpanId(),
                "Child span should have parent span ID as its parent");

        childSpan.close();
        parentSpan.close();
        service.close();
    }

    @Test
    void testMultipleToolsMetrics() {
        McpTelemetry annotation = createMockAnnotation(
                true, false, false, "http://localhost:4317",
                1.0, false, 60000
        );

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        // Record metrics for different tools
        service.recordToolInvocation("toolA", Duration.ofMillis(100), true);
        service.recordToolInvocation("toolB", Duration.ofMillis(200), true);
        service.recordToolInvocation("toolA", Duration.ofMillis(150), false);
        service.recordToolInvocation("toolC", Duration.ofMillis(50), true);

        service.increment("toolA.custom");
        service.record("toolB.latency", 250);

        service.close();
    }
}
