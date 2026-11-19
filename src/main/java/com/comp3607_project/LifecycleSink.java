package com.comp3607_project;

/**
 * Where lifecycle events are sent once created. Implementations must return quickly, because
 * they run on BlueJ's listener threads and the watcher thread. In the plugin, {@link
 * TelemetryFacade} supplies a lambda that wraps each event in a {@link LifecycleEventAdapter}
 * and records it in the {@link FileEventSink}.
 */
//Where lifecycle events go once created. 
@FunctionalInterface
public interface LifecycleSink {

        /**
     * Accepts one lifecycle event.
     *
     * @param event the lifecycle event
     */
    void accept(LifecycleEvent event);
}
