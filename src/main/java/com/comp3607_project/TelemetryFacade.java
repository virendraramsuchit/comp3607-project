package com.comp3607_project;

import java.io.IOException;

import bluej.extensions2.BlueJ;

/**
 * Builds and connects every part of the plugin, registers the listeners with BlueJ, and
 * exposes just two operations, {@code start()} and {@code stop()}. {@link TelemetryExtension}
 * only talks to this class.
 */
public class TelemetryFacade {

        /**
     * Receives every event and writes the outputs.
     */
    private final FileEventSink sink;

        /**
     * Checks open editors for edits.
     */
    private final EditPoller editPoller;

        /**
     * Tracks project and class lifecycle events.
     */
    private final LifecycleCollector lifecycle;

        /**
     * Records compile events.
     */
    private final CompileTracker compileTracker;

        /**
     * Attaches typing listeners to class editors.
     */
    private final ClassTracker classTracker;

        /**
     * Attaches typing listeners when packages open and records activity when they close.
     */
    private final PackageTracker packageTracker;

        /**
     * Calculates active and idle time from keystrokes.
     */
    private final ActivityTracker activityTracker;

        /**
     * Creates the sink, collectors and lifecycle collector. Registers {@code ExecutionListener},
     * {@code ClassTracker}, {@code PackageTracker} and {@code CompileTracker} with BlueJ.
     * Lifecycle events are wrapped in a {@link LifecycleEventAdapter} before being sent to the
     * sink.
     *
     * @param bluej the BlueJ proxy passed to the extension
     * @throws IOException if the folder watcher cannot be created
     */
    public TelemetryFacade(BlueJ bluej) throws IOException {
        
        sink = new FileEventSink();

        bluej.addInvocationFinishedListener(new ExecutionListener(sink));

        editPoller = new EditPoller(bluej, sink);

        activityTracker = new ActivityTracker();
        classTracker    = new ClassTracker(activityTracker);
        packageTracker  = new PackageTracker(activityTracker, classTracker, sink);
        compileTracker  = new CompileTracker(sink);

        bluej.addClassListener(classTracker);
        bluej.addPackageListener(packageTracker);
        bluej.addCompileListener(compileTracker);

        LifecycleSink lifecycleSink = event -> sink.record(new LifecycleEventAdapter(event));
        lifecycle = new LifecycleCollector(bluej, lifecycleSink);
    }

        /**
     * Starts the edit poller and registers the lifecycle collector.
     */
    public void start() {
        editPoller.start();
        lifecycle.register();
    }

        /**
     * Stops the edit poller, shuts down the lifecycle collector (recording the close of any open
     * projects), then exports the PDF report.
     */
    public void stop() {
        editPoller.stop();
        lifecycle.shutdown();
        sink.exportPdf();
    }
}