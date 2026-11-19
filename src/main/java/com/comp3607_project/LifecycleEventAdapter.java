package com.comp3607_project;

/**
 * Wraps a {@link LifecycleEvent} so it can be stored and reported like any other {@link
 * TelemetryEvent}, without changing either class. {@code getTimeStamp()} returns the wrapped
 * event's {@code getEpochMillis()}.
 */
public class LifecycleEventAdapter implements TelemetryEvent {

        /**
     * The adapted lifecycle event.
     */
    private final LifecycleEvent wrapped;

        /**
     * The wrapped event's time, copied when the adapter is created.
     */
    private final long timestamp;

        /**
     * Wraps {@code event}.
     *
     * @param event the lifecycle event to wrap
     */
    public LifecycleEventAdapter(LifecycleEvent event) {
        this.wrapped = event;
        this.timestamp = event.getEpochMillis();
    }

        /**
     * Returns the wrapped event, used by the report strategies to read its type and details.
     * @return the wrapped event, used by the report strategies to read its type and details
     */
    public LifecycleEvent getWrapped() { return wrapped; }

        /**
     * Returns the wrapped event's time.
     * @return the wrapped event's time
     */
    @Override public long getTimeStamp() { return timestamp; }

        /**
     * Returns {@code LIFECYCLE } followed by the wrapped event's text.
     * @return a one-line description of the event
     */
    @Override public String toString() {
        return "LIFECYCLE " + wrapped.toString();
    }
}
