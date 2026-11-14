package com.comp3607_project.report;

import java.util.List;

import com.comp3607_project.TelemetryEvent;

public interface ReportStrategy {

    void write(List<TelemetryEvent> events);
    
}
