package com.comp3607_project;

import bluej.extensions2.BClass;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;

public class PackageTracker implements PackageListener {

    private final ActivityTracker tracker;
    private final ClassTracker classTracker;
    private final EventSink sink;   

    public PackageTracker(ActivityTracker tracker, ClassTracker classTracker, EventSink sink) {
        this.tracker = tracker;
        this.classTracker = classTracker;
        this.sink = sink;
    }

    // KEPT: old constructor for backwards-compatible tests, using a no-op sink
    public PackageTracker(ActivityTracker tracker, ClassTracker classTracker) {
        this(tracker, classTracker, event -> { /* no-op */ });
    }

    @Override
    public void packageClosing(PackageEvent event) {
        try {
            String pkgName = event.getPackage().getName();
            long active = tracker.getActiveTime();
            long idle = tracker.getIdleTime();

            System.out.println("Package closing: " + pkgName);
            System.out.println("Active: " + active + " ms | Idle: " + idle + " ms");

            sink.record(new ActivityEvent(pkgName, active, idle));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void packageOpened(PackageEvent event) {
        try {
            System.out.println("Package opened: " + event.getPackage().getName());
            for (BClass c : event.getPackage().getClasses()) {
                classTracker.attach(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

