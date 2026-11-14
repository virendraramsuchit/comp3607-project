package com.comp3607_project;

public class LifecycleEventAdapter implements TelemetryEvent {

    private final LifecycleEvent wrapped;
    private final long timestamp;

    public LifecycleEventAdapter(LifecycleEvent event) {
        this.wrapped = event;
        this.timestamp = event.getEpochMillis();
    }

    public LifecycleEvent getWrapped() { return wrapped; }

    @Override public long getTimeStamp() { return timestamp; }

    @Override public String toString() {
        return "LIFECYCLE " + wrapped.toString();
    }
}
