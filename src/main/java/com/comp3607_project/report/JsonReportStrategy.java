package com.comp3607_project.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.comp3607_project.ActivityEvent;
import com.comp3607_project.CompileEvent;
import com.comp3607_project.EditEvent;
import com.comp3607_project.ExecutionEvent;
import com.comp3607_project.LifecycleEvent;
import com.comp3607_project.LifecycleEventAdapter;
import com.comp3607_project.TelemetryEvent;

public class JsonReportStrategy implements ReportStrategy {

    private final Path jsonFile;

    public JsonReportStrategy() {
        this.jsonFile = Path.of(System.getProperty("user.home"), "telemetry-output.json");
    }

    @Override
    public void write(List<TelemetryEvent> events) {
        StringBuilder sb = new StringBuilder("{\n  \"events\": [\n");
        for (int i = 0; i < events.size(); i++) {
            sb.append("    ").append(serialize(events.get(i)));
            if (i < events.size() - 1) sb.append(',');
            sb.append('\n');
        }
        sb.append("  ],\n");
        sb.append("  \"summary\": ").append(summaryJson(events)).append("\n}\n");

        try {
            Files.writeString(jsonFile, sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            System.err.println("JSON write failed: " + ex.getMessage());
        }
    }

    private String serialize(TelemetryEvent e) {
        if (e instanceof EditEvent ed) {
            return String.format(
                "{\"type\":\"EDIT\",\"timestamp\":%d,\"timestampIso\":\"%s\",\"class\":\"%s\",\"charsChanged\":%d,\"idleMs\":%d}",
                ed.getTimeStamp(), isoOf(ed.getTimeStamp()),
                ed.getClassName(), ed.getNumCharsChanged(), ed.getIdleMillis());
        }
        if (e instanceof ExecutionEvent ex) {
            String m = ex.getMethodName() == null ? "constructor" : ex.getMethodName();
            return String.format(
                "{\"type\":\"RUN\",\"timestamp\":%d,\"timestampIso\":\"%s\",\"class\":\"%s\",\"method\":\"%s\",\"outcome\":\"%s\"}",
                ex.getTimeStamp(), isoOf(ex.getTimeStamp()),
                ex.getClassName(), m, ex.getOutcome());
        }
        if (e instanceof LifecycleEventAdapter la) {
            LifecycleEvent w = la.getWrapped();
            return String.format(
                "{\"type\":\"LIFECYCLE\",\"timestamp\":%d,\"timestampIso\":\"%s\"," +
                "\"eventType\":\"%s\",\"project\":\"%s\",\"package\":\"%s\"," +
                "\"className\":\"%s\",\"previousClassName\":\"%s\",\"detail\":\"%s\"}",
                la.getTimeStamp(), isoOf(la.getTimeStamp()),
                esc(w.getType() == null ? "" : w.getType().name()),
                esc(w.getProjectName()),
                esc(w.getPackageName()),
                esc(w.getClassName()),
                esc(w.getPreviousClassName()),
                esc(w.getDetail()));
        }
        if (e instanceof CompileEvent ce) {
            return String.format(
                "{\"type\":\"COMPILE\",\"timestamp\":%d,\"timestampIso\":\"%s\"," +
                "\"outcome\":\"%s\",\"file\":\"%s\",\"line\":%d,\"message\":\"%s\",\"errorKey\":\"%s\"}",
                ce.getTimeStamp(), isoOf(ce.getTimeStamp()),
                ce.getOutcome().name(),
                esc(ce.getFileName()),
                ce.getLineNumber(),
                esc(ce.getMessage()),
                esc(ce.getErrorKey()));
        }
        if (e instanceof ActivityEvent ae) {
            return String.format(
                "{\"type\":\"ACTIVITY\",\"timestamp\":%d,\"timestampIso\":\"%s\"," +
                "\"package\":\"%s\",\"activeMs\":%d,\"idleMs\":%d}",
                ae.getTimeStamp(), isoOf(ae.getTimeStamp()),
                esc(ae.getPackageName()),
                ae.getActiveMillis(),
                ae.getIdleMillis());
        }
        return "{\"type\":\"UNKNOWN\"}";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String isoOf(long epochMillis) {
        return java.time.Instant.ofEpochMilli(epochMillis).toString();
    }

    private static String formatDuration(long ms) {
        long s = ms / 1000;
        if (s < 60) return s + "s";
        return (s / 60) + "m " + (s % 60) + "s";
    }

    private String summaryJson(List<TelemetryEvent> events) {
        if (events.isEmpty()) return "{}";

        long start = events.get(0).getTimeStamp();
        long end   = events.get(events.size() - 1).getTimeStamp();
        long dur   = end - start;

        long idleMs = 0;
        int totalEdits = 0, totalRuns = 0, totalExceptions = 0;
        int totalLifecycle = 0;
        int totalCompileSuccess = 0;
        int totalCompileFailure = 0;
        long lastActiveMillis = 0;
        long lastIdleMillis = 0;

        Map<String,Integer> lifecycleByType = new HashMap<>();
        Map<String,Integer> editsPerClass     = new HashMap<>();
        Map<String,Integer> runsPerClass      = new HashMap<>();
        Map<String,Integer> exceptionsPerClass = new HashMap<>();
        Map<String,Integer> compileErrorsByKey = new HashMap<>();

        for (TelemetryEvent e : events) {
            if (e instanceof EditEvent ed) {
                totalEdits++;
                idleMs += ed.getIdleMillis();
                editsPerClass.merge(ed.getClassName(), ed.getNumCharsChanged(), Integer::sum);
            } else if (e instanceof ExecutionEvent ex) {
                totalRuns++;
                runsPerClass.merge(ex.getClassName(), 1, Integer::sum);
                if ("EXCEPTION".equals(ex.getOutcome())) {
                    totalExceptions++;
                    exceptionsPerClass.merge(ex.getClassName(), 1, Integer::sum);
                }
            } else if (e instanceof LifecycleEventAdapter la) {
                totalLifecycle++;
                lifecycleByType.merge(la.getWrapped().getType().name(), 1, Integer::sum);
            } else if (e instanceof CompileEvent ce) {
                switch (ce.getOutcome()) {
                    case SUCCESS -> totalCompileSuccess++;
                    case FAILURE -> totalCompileFailure++;
                    case ERROR   -> compileErrorsByKey.merge(ce.getErrorKey(), 1, Integer::sum);
                    default -> { }
                }
            } else if (e instanceof ActivityEvent ae) {
                lastActiveMillis = ae.getActiveMillis();
                lastIdleMillis = ae.getIdleMillis();
            }
        }

        long activeMs = Math.max(0, dur - idleMs);
        double idlePct = dur == 0 ? 0 : 100.0 * idleMs / dur;
        double exRate  = totalRuns == 0 ? 0 : 100.0 * totalExceptions / totalRuns;

        return String.format(
            "{\n" +
            "    \"sessionDurationMs\": %d,\n" +
            "    \"sessionDurationFormatted\": \"%s\",\n" +
            "    \"totalEdits\": %d,\n" +
            "    \"session\": {\n" +
            "      \"activeMs\": %d,\n" +
            "      \"activeFormatted\": \"%s\",\n" +
            "      \"idleMs\": %d,\n" +
            "      \"idleFormatted\": \"%s\",\n" +
            "      \"idlePercentage\": %.2f\n" +
            "    },\n" +
            "    \"keystrokes\": {\n" +
            "      \"activeMs\": %d,\n" +
            "      \"activeFormatted\": \"%s\",\n" +
            "      \"idleMs\": %d,\n" +
            "      \"idleFormatted\": \"%s\",\n" +
            "      \"idleThresholdMs\": 10000\n" +
            "    },\n" +
            "    \"totalLifecycleEvents\": %d,\n" +
            "    \"lifecycleByType\": %s,\n" +
            "    \"compilation\": {\n" +
            "      \"successes\": %d,\n" +
            "      \"failures\": %d,\n" +
            "      \"errorBreakdown\": %s\n" +
            "    },\n" +
            "    \"totalRuns\": %d,\n" +
            "    \"totalExceptions\": %d,\n" +
            "    \"exceptionRatePercent\": %.2f,\n" +
            "    \"editsPerClass\": %s,\n" +
            "    \"runsPerClass\": %s,\n" +
            "    \"exceptionsPerClass\": %s\n" +
            "  }",
            dur, formatDuration(dur), totalEdits,
            activeMs, formatDuration(activeMs),
            idleMs, formatDuration(idleMs), idlePct,
            lastActiveMillis, formatDuration(lastActiveMillis),
            lastIdleMillis, formatDuration(lastIdleMillis),
            totalLifecycle, mapJson(lifecycleByType),
            totalCompileSuccess, totalCompileFailure, mapJson(compileErrorsByKey),
            totalRuns, totalExceptions, exRate,
            mapJson(editsPerClass), mapJson(runsPerClass), mapJson(exceptionsPerClass));
    }

    private String mapJson(Map<String,Integer> m) {
        if (m.isEmpty()) return "{}";
        return m.entrySet().stream()
            .map(e -> "\"" + e.getKey() + "\":" + e.getValue())
            .collect(Collectors.joining(",", "{", "}"));
    }

}
