package com.comp3607_project.report;

import java.util.List;

import com.comp3607_project.FileEventSink;
import com.comp3607_project.TelemetryEvent;

/**
 * A way of writing a report from the session's events. {@link FileEventSink} uses one strategy
 * for JSON and one for PDF.
 */
public interface ReportStrategy {

        /**
     * Writes a report for the given events.
     *
     * @param events the session's events, in order
     */
    void write(List<TelemetryEvent> events);
    
}
