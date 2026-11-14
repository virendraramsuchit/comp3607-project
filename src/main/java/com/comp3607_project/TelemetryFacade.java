package com.comp3607_project;

import java.io.IOException;

import bluej.extensions2.BlueJ;

public class TelemetryFacade {

    private final FileEventSink sink;
    private final EditPoller editPoller;
    private final LifecycleCollector lifecycle;
    private final CompileTracker compileTracker;
    private final ClassTracker classTracker;
    private final PackageTracker packageTracker;
    private final ActivityTracker activityTracker;

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

    public void start() {
        editPoller.start();
        lifecycle.register();
    }

    public void stop() {
        editPoller.stop();
        lifecycle.shutdown();
        sink.exportPdf();
    }
}