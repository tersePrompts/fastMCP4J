package com.ultrathink.fastmcp.telemetry;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Console-based telemetry exporter.
 * <p>
 * Outputs metrics and traces to the console in a human-readable format.
 */
public class ConsoleTelemetryExporter implements java.util.function.Consumer<TelemetryService.TelemetryData> {
    private static final Logger log = LoggerFactory.getLogger(ConsoleTelemetryExporter.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Override
    public void accept(TelemetryService.TelemetryData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔═══════════════════════════════════════════════════════════════╗\n");
        sb.append(String.format("║ Telemetry Report: %-40s ║\n", data.serverName()));
        sb.append(String.format("║ Time: %-52s ║\n", data.timestamp().format(FORMATTER)));
        sb.append("╠═══════════════════════════════════════════════════════════════╣\n");

        // Counters
        if (!data.counters().isEmpty()) {
            sb.append("║ Counters:                                                  ║\n");
            sb.append("╠───────────────────────────────────────────────────────────╣\n");
            data.counters().forEach((name, counter) -> {
                sb.append(String.format("║  %-40s %15d ║\n", name, counter.get()));
            });
            sb.append("╠───────────────────────────────────────────────────────────╣\n");
        }

        // Histograms
        if (!data.histograms().isEmpty()) {
            sb.append("║ Histograms:                                                ║\n");
            sb.append("╠───────────────────────────────────────────────────────────╣\n");
            sb.append("║  Name                                    count    avg    ║\n");
            sb.append("╠───────────────────────────────────────────────────────────╣\n");
            data.histograms().forEach((name, hist) -> {
                TelemetryService.MetricHistogram.Snapshot snap = hist.getSnapshot();
                sb.append(String.format("║  %-40s %5d %6dms ║\n",
                    name, snap.count(), snap.avg()));
            });
        }

        sb.append("╚═══════════════════════════════════════════════════════════════╝\n");

        log.info(sb.toString());
    }
}
