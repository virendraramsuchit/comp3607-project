package com.comp3607_project;

import bluej.extensions2.BClass;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;

/**
 * When a package opens, attaches typing listeners to all its classes. When a package closes,
 * records an {@link ActivityEvent} with the active and idle time so far.
 */
public class PackageTracker implements PackageListener {

        /**
     * Source of active and idle time.
     */
    private final ActivityTracker tracker;

        /**
     * Used to attach listeners to each class.
     */
    private final ClassTracker classTracker;

        /**
     * Where activity events are sent.
     */
    private final EventSink sink;   

        /**
     * Creates a package tracker that sends activity events to {@code sink}.
     *
     * @param tracker the tracker that receives the data
     * @param classTracker the class tracker used to attach typing listeners
     * @param sink where events are sent
     */
    public PackageTracker(ActivityTracker tracker, ClassTracker classTracker, EventSink sink) {
        this.tracker = tracker;
        this.classTracker = classTracker;
        this.sink = sink;
    }

        /**
     * Same, with a sink that discards events (used by unit tests).
     *
     * @param tracker the tracker that receives the data
     * @param classTracker the class tracker used to attach typing listeners
     */
    // KEPT: old constructor for backwards-compatible tests, using a no-op sink
    public PackageTracker(ActivityTracker tracker, ClassTracker classTracker) {
        this(tracker, classTracker, event -> { /* no-op */ });
    }

        /**
     * Records an {@code ActivityEvent} for the package with the current active and idle time.
     *
     * @param event BlueJ's package notification
     */
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

        /**
     * Calls {@code classTracker.attach} for every class in the package.
     *
     * @param event BlueJ's package notification
     */
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

