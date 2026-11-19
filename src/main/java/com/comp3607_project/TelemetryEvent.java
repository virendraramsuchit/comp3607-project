package com.comp3607_project;

/**
 * The common interface of every event the plugin records, so they can all be stored in one
 * list and sent to one {@link EventSink}.
 */
public interface TelemetryEvent {

        /**
     * The time the event happened, in milliseconds since 1 January 1970 (UTC).
     * @return the event time in ms since 1 January 1970 UTC
     */
    long getTimeStamp();
    
}