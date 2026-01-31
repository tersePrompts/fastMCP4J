package com.ultrathink.fastmcp.telemetry;

import com.ultrathink.fastmcp.annotations.McpTelemetry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryServiceTest {

    @Test
    void testCreateTelemetryServiceWithDefaults() {
        McpTelemetry annotation = new McpTelemetry() {
            @Override
            public boolean enabled() { return true; }

            @Override
            public boolean exportConsole() { return true; }

            @Override
            public boolean exportOtlp() { return false; }

            @Override
            public String otlpEndpoint() { return "http://localhost:4317"; }

            @Override
            public double sampleRate() { return 1.0; }

            @Override
            public boolean includeArguments() { return false; }

            @Override
            public long metricExportIntervalMs() { return 60000; }

            @Override
            public Class<?> annotationType() { return McpTelemetry.class; }
        };

        TelemetryService service = TelemetryService.create("TestServer", annotation);
        assertNotNull(service, "TelemetryService should be created");
        service.close();
    }

    @Test
    void testTelemetryDisabled() {
        McpTelemetry annotation = new McpTelemetry() {
            @Override
            public boolean enabled() { return false; }

            @Override
            public boolean exportConsole() { return true; }

            @Override
            public boolean exportOtlp() { return false; }

            @Override
            public String otlpEndpoint() { return "http://localhost:4317"; }

            @Override
            public double sampleRate() { return 1.0; }

            @Override
            public boolean includeArguments() { return false; }

            @Override
            public long metricExportIntervalMs() { return 60000; }

            @Override
            public Class<?> annotationType() { return McpTelemetry.class; }
        };

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        // When disabled, increment should be a no-op
        service.increment("test.counter");
        service.record("test.histogram", 100);
        service.recordToolInvocation("testTool", Duration.ofMillis(100), true);

        // Should not throw
        service.close();
    }

    @Test
    void testRecordToolInvocation() {
        McpTelemetry annotation = new McpTelemetry() {
            @Override
            public boolean enabled() { return true; }

            @Override
            public boolean exportConsole() { return false; } // Disable console output

            @Override
            public boolean exportOtlp() { return false; }

            @Override
            public String otlpEndpoint() { return "http://localhost:4317"; }

            @Override
            public double sampleRate() { return 1.0; }

            @Override
            public boolean includeArguments() { return false; }

            @Override
            public long metricExportIntervalMs() { return 60000; }

            @Override
            public Class<?> annotationType() { return McpTelemetry.class; }
        };

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        // Record some tool invocations
        service.recordToolInvocation("search", Duration.ofMillis(50), true);
        service.recordToolInvocation("search", Duration.ofMillis(75), true);
        service.recordToolInvocation("search", Duration.ofMillis(100), false);

        service.close();
        // If no exception is thrown, test passes
    }

    @Test
    void testSpanSampling() {
        McpTelemetry annotation = new McpTelemetry() {
            @Override
            public boolean enabled() { return true; }

            @Override
            public boolean exportConsole() { return false; }

            @Override
            public boolean exportOtlp() { return false; }

            @Override
            public String otlpEndpoint() { return "http://localhost:4317"; }

            @Override
            public double sampleRate() { return 0.0; } // 0% sampling

            @Override
            public boolean includeArguments() { return false; }

            @Override
            public long metricExportIntervalMs() { return 60000; }

            @Override
            public Class<?> annotationType() { return McpTelemetry.class; }
        };

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        // With 0% sample rate, span should always be null
        TelemetryService.Span span = service.createSpan("testOperation", null);
        assertNull(span, "Span should be null with 0% sampling rate");

        service.close();
    }

    @Test
    void testSpanSamplingFull() {
        McpTelemetry annotation = new McpTelemetry() {
            @Override
            public boolean enabled() { return true; }

            @Override
            public boolean exportConsole() { return false; }

            @Override
            public boolean exportOtlp() { return false; }

            @Override
            public String otlpEndpoint() { return "http://localhost:4317"; }

            @Override
            public double sampleRate() { return 1.0; } // 100% sampling

            @Override
            public boolean includeArguments() { return false; }

            @Override
            public long metricExportIntervalMs() { return 60000; }

            @Override
            public Class<?> annotationType() { return McpTelemetry.class; }
        };

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
        McpTelemetry annotation = new McpTelemetry() {
            @Override
            public boolean enabled() { return true; }

            @Override
            public boolean exportConsole() { return false; }

            @Override
            public boolean exportOtlp() { return false; }

            @Override
            public String otlpEndpoint() { return "http://localhost:4317"; }

            @Override
            public double sampleRate() { return 1.0; }

            @Override
            public boolean includeArguments() { return false; }

            @Override
            public long metricExportIntervalMs() { return 60000; }

            @Override
            public Class<?> annotationType() { return McpTelemetry.class; }
        };

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        try (TelemetryService.Span span = service.createSpan("testOperation", null)) {
            assertNotNull(span, "Span should be created");
            // Simulate some work
        }

        service.close();
    }

    @Test
    void testCounterIncrement() {
        McpTelemetry annotation = new McpTelemetry() {
            @Override
            public boolean enabled() { return true; }

            @Override
            public boolean exportConsole() { return false; }

            @Override
            public boolean exportOtlp() { return false; }

            @Override
            public String otlpEndpoint() { return "http://localhost:4317"; }

            @Override
            public double sampleRate() { return 1.0; }

            @Override
            public boolean includeArguments() { return false; }

            @Override
            public long metricExportIntervalMs() { return 60000; }

            @Override
            public Class<?> annotationType() { return McpTelemetry.class; }
        };

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        service.increment("requests.total");
        service.increment("requests.total");
        service.increment("requests.total", 5);

        service.close();
    }

    @Test
    void testHistogramRecord() {
        McpTelemetry annotation = new McpTelemetry() {
            @Override
            public boolean enabled() { return true; }

            @Override
            public boolean exportConsole() { return false; }

            @Override
            public boolean exportOtlp() { return false; }

            @Override
            public String otlpEndpoint() { return "http://localhost:4317"; }

            @Override
            public double sampleRate() { return 1.0; }

            @Override
            public boolean includeArguments() { return false; }

            @Override
            public long metricExportIntervalMs() { return 60000; }

            @Override
            public Class<?> annotationType() { return McpTelemetry.class; }
        };

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        service.record("latency.ms", 10);
        service.record("latency.ms", 50);
        service.record("latency.ms", 100);
        service.record("latency.ms", 25);

        service.close();
    }

    @Test
    void testTraceContextMDC() {
        McpTelemetry annotation = new McpTelemetry() {
            @Override
            public boolean enabled() { return true; }

            @Override
            public boolean exportConsole() { return false; }

            @Override
            public boolean exportOtlp() { return false; }

            @Override
            public String otlpEndpoint() { return "http://localhost:4317"; }

            @Override
            public double sampleRate() { return 1.0; }

            @Override
            public boolean includeArguments() { return false; }

            @Override
            public long metricExportIntervalMs() { return 60000; }

            @Override
            public Class<?> annotationType() { return McpTelemetry.class; }
        };

        TelemetryService service = TelemetryService.create("TestServer", annotation);

        service.setTraceContext("trace-123", "span-456");
        service.clearTraceContext();

        service.close();
    }
}
