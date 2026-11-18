package com.comp3607_project;
/**
 * Where every collector sends its events. Collectors depend on this interface rather than on
 * {@link FileEventSink}, so tests can pass in a simple replacement.
 */
public interface EventSink {
/**
 * Accepts one event.
 *
 * @param event the event to record
 */
    void record(TelemetryEvent event);
    
}
