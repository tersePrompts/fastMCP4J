package com.ultrathink.fastmcp.telemetry;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.ultrathink.fastmcp.annotations.McpTelemetry;

/**
 * Core telemetry service for MCP servers.
 * <p>
 * Collects metrics and traces without external dependencies.
 * Thread-safe and designed for minimal overhead.
 */
public class TelemetryService implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final String serverName;
    private final TelemetryConfig config;
    private final ConcurrentMap<String, MetricCounter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MetricHistogram> histograms = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final Consumer<TelemetryData> exporter;

    private TelemetryService(String serverName, TelemetryConfig config, Consumer<TelemetryData> exporter) {
        this.serverName = serverName;
        this.config = config;
        this.exporter = exporter;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "telemetry-exporter");
            t.setDaemon(true);
            return t;
        });

        // Schedule periodic metric export
        scheduler.scheduleAtFixedRate(
            this::exportMetrics,
            config.metricExportIntervalMs(),
            config.metricExportIntervalMs(),
            TimeUnit.MILLISECONDS
        );
    }

    public static TelemetryService create(String serverName, McpTelemetry annotation) {
        TelemetryConfig config = new TelemetryConfig(
            annotation.enabled(),
            annotation.exportConsole(),
            annotation.exportOtlp(),
            annotation.otlpEndpoint(),
            annotation.sampleRate(),
            annotation.includeArguments(),
            annotation.metricExportIntervalMs()
        );

        Consumer<TelemetryData> exporter;
        if (config.exportConsole()) {
            exporter = new ConsoleTelemetryExporter();
        } else if (config.exportOtlp()) {
            exporter = createOtlpExporter(config);
        } else {
            exporter = data -> {}; // no-op
        }

        return new TelemetryService(serverName, config, exporter);
    }

    private static Consumer<TelemetryData> createOtlpExporter(TelemetryConfig config) {
        try {
            // Try to load OTLP exporter if available
            Class<?> exporterClass = Class.forName("com.ultrathink.fastmcp.telemetry.OtlpTelemetryExporter");
            return (Consumer<TelemetryData>) exporterClass
                .getConstructor(TelemetryConfig.class)
                .newInstance(config);
        } catch (Exception e) {
            log.warn("OTLP exporter not available, falling back to console", e);
            return new ConsoleTelemetryExporter();
        }
    }

    /**
     * Record a tool invocation.
     */
    public void recordToolInvocation(String toolName, Duration duration, boolean success) {
        if (!config.enabled()) return;

        counters.computeIfAbsent("tool." + toolName + ".calls", k -> new MetricCounter()).increment();
        counters.computeIfAbsent("tool." + toolName + (success ? ".success" : ".error"), k -> new MetricCounter()).increment();
        histograms.computeIfAbsent("tool." + toolName + ".duration_ms", k -> new MetricHistogram())
            .record(duration.toMillis());
    }

    /**
     * Increment a counter metric.
     */
    public void increment(String name) {
        if (!config.enabled()) return;
        counters.computeIfAbsent(name, k -> new MetricCounter()).increment();
    }

    /**
     * Record a value in a histogram.
     */
    public void record(String name, long value) {
        if (!config.enabled()) return;
        histograms.computeIfAbsent(name, k -> new MetricHistogram()).record(value);
    }

    /**
     * Create a span for tracing.
     * Returns {@code null} if sampling is disabled.
     */
    public Span createSpan(String name, String parentSpanId) {
        if (!config.enabled() || Math.random() > config.sampleRate()) {
            return null;
        }
        return new Span(name, parentSpanId);
    }

    /**
     * Set trace context in MDC for the current request.
     */
    public void setTraceContext(String traceId, String spanId) {
        if (traceId != null) MDC.put("traceId", traceId);
        if (spanId != null) MDC.put("spanId", spanId);
    }

    /**
     * Clear trace context from MDC.
     */
    public void clearTraceContext() {
        MDC.remove("traceId");
        MDC.remove("spanId");
    }

    private void exportMetrics() {
        if (!config.enabled() || counters.isEmpty() && histograms.isEmpty()) return;

        TelemetryData data = new TelemetryData(serverName, Instant.now(),
            new ConcurrentHashMap<>(counters),
            new ConcurrentHashMap<>(histograms));

        try {
            exporter.accept(data);
        } catch (Exception e) {
            log.warn("Failed to export telemetry", e);
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        exportMetrics(); // Final export
    }

    /**
     * Telemetry configuration.
     */
    public record TelemetryConfig(
        boolean enabled,
        boolean exportConsole,
        boolean exportOtlp,
        String otlpEndpoint,
        double sampleRate,
        boolean includeArguments,
        long metricExportIntervalMs
    ) {}

    /**
     * A simple counter for metrics.
     */
    public static class MetricCounter {
        private final AtomicLong value = new AtomicLong();

        public void increment() {
            value.incrementAndGet();
        }

        public void increment(long delta) {
            value.addAndGet(delta);
        }

        public long get() {
            return value.get();
        }
    }

    /**
     * A simple histogram for recording value distributions.
     */
    public static class MetricHistogram {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong sum = new AtomicLong();
        private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

        public void record(long value) {
            count.incrementAndGet();
            sum.addAndGet(value);

            // Update min/max (simple CAS loop)
            long currentMin;
            do {
                currentMin = min.get();
                if (value >= currentMin) break;
            } while (!min.compareAndSet(currentMin, value));

            long currentMax;
            do {
                currentMax = max.get();
                if (value <= currentMax) break;
            } while (!max.compareAndSet(currentMax, value));
        }

        public Snapshot getSnapshot() {
            long c = count.get();
            return new Snapshot(
                c,
                c > 0 ? sum.get() / c : 0,
                min.get() == Long.MAX_VALUE ? 0 : min.get(),
                max.get() == Long.MIN_VALUE ? 0 : max.get()
            );
        }

        public record Snapshot(long count, long avg, long min, long max) {}
    }

    /**
     * A span representing a single operation in a trace.
     */
    public class Span implements AutoCloseable {
        private final String name;
        private final String spanId;
        private final String parentSpanId;
        private final Instant startTime;
        private final String traceId;

        private Span(String name, String parentSpanId) {
            this.name = name;
            this.spanId = generateSpanId();
            this.parentSpanId = parentSpanId;
            this.startTime = Instant.now();
            this.traceId = parentSpanId != null ? MDC.get("traceId") : generateTraceId();

            // Set MDC for logging
            setTraceContext(traceId, spanId);
        }

        public String getSpanId() {
            return spanId;
        }

        public String getTraceId() {
            return traceId;
        }

        public String getParentSpanId() {
            return parentSpanId;
        }

        public void recordAttribute(String key, String value) {
            if (!config.includeArguments()) return;
            // Attributes could be stored here for export
        }

        public void recordEvent(String eventName) {
            // Events could be stored here for export
        }

        @Override
        public void close() {
            Duration duration = Duration.between(startTime, Instant.now());
            clearTraceContext();

            // Record the span duration
            record("span." + name + ".duration_ms", duration.toMillis());
        }
    }

    /**
     * Container for telemetry data export.
     */
    public record TelemetryData(
        String serverName,
        Instant timestamp,
        ConcurrentMap<String, MetricCounter> counters,
        ConcurrentMap<String, MetricHistogram> histograms
    ) {}

    private static String generateTraceId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static String generateSpanId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
