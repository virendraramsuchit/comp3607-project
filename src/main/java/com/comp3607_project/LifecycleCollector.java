package com.comp3607_project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.BlueJ;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;

/**
 * Connects BlueJ's package and class notifications to the {@link ProjectTracker}, and starts
 * the {@link ProjectWatcher} so changes made outside BlueJ are also seen. It never throws back
 * into BlueJ: every listener method catches and logs its own errors.
 */
public final class LifecycleCollector implements PackageListener, ClassListener {

        /**
     * Used to register and remove the listeners.
     */
    private final BlueJ bluej;

        /**
     * Turns changes into lifecycle events.
     */
    private final ProjectTracker tracker;

        /**
     * Watches open package folders.
     */
    private final ProjectWatcher watcher;

        /**
     * Creates a tracker that sends events to {@code sink} and a watcher with a 500 ms debounce.
     *
     * @param bluej the BlueJ proxy passed to the extension
     * @param sink where events are sent
     * @throws IOException if the file watcher cannot be created
     */
    public LifecycleCollector(BlueJ bluej, LifecycleSink sink) throws IOException {
        this.bluej = bluej;
        this.tracker = new ProjectTracker(sink);
        this.watcher = new ProjectWatcher(tracker, ProjectWatcher.DEFAULT_DEBOUNCE_MILLIS);
    }

        /**
     * Starts the watcher and registers as a package and class listener. Call from startup.
     */
    // Call from Extension.startup(BlueJ).
    public void register() {
        watcher.start();
        bluej.addPackageListener(this);
        bluej.addClassListener(this);
    }

        /**
     * Removes the listeners, closes the watcher and records the close of every open project. Safe
     * to call more than once.
     */
    // Call from Extension.terminate(). 
    public void shutdown() {
        try {
            bluej.removePackageListener(this);
            bluej.removeClassListener(this);
        } catch (Exception ignored) {
            // the extension may already be unloaded
        }
        watcher.close();
        tracker.shutdown("session-end");
    }

    //PackageListener

        /**
     * Collects the package's classes from BlueJ and from disk, checks whether the project looks
     * new, informs the tracker and starts watching the folder.
     *
     * @param ev BlueJ's package notification
     */
    @Override
    public void packageOpened(PackageEvent ev) {
        try {
            BPackage pkg = ev.getPackage();
            BProject project = pkg.getProject();
            Path pkgDir = pkg.getDir().toPath();
            Path projectDir = project.getDir().toPath();
            String pkgName = pkg.getName();

            List<String> names = new ArrayList<>();
            for (BClass c : pkg.getClasses()) {
                try {
                    names.add(c.getName());
                } catch (Exception skip) {
                    // one broken wrapper must not hide the others
                }
            }
            Map<String, String> onDisk = SourceFiles.snapshot(pkgDir);
            boolean isNew = NewProjectHeuristic.looksNew(projectDir, pkgName,
                    Math.max(names.size(), onDisk.size()), System.currentTimeMillis());
            tracker.packageOpened(pkgDir, projectDir, project.getName(), pkgName, names, onDisk, isNew);
            watcher.watch(pkgDir);
        } catch (Exception e) { 
            log("packageOpened", e);
        }
    }

        /**
     * Stops watching the folder and informs the tracker.
     *
     * @param ev BlueJ's package notification
     */
    @Override
    public void packageClosing(PackageEvent ev) {
        try {
            Path pkgDir = ev.getPackage().getDir().toPath();
            watcher.unwatch(pkgDir);
            tracker.packageClosing(pkgDir);
        } catch (Exception e) {
            log("packageClosing", e);
        }
    }

    // ClassListener

        /**
     * Reports the class to the tracker (recorded as created the first time) and requests a folder
     * scan to pick up saved content changes.
     *
     * @param ev BlueJ's class notification
     */
    @Override
    public void classStateChanged(ClassEvent ev) {
        try {
            Path dir = dirOf(ev);
            tracker.classSeen(dir, ev.getBClass().getName());
            watcher.requestReconcile(dir);
        } catch (Exception e) {
            log("classStateChanged", e);
        }
    }

        /**
     * Reports a rename to the tracker.
     *
     * @param ev BlueJ's class notification
     */
    @Override
    public void classNameChanged(ClassEvent ev) {
        try {
            tracker.classRenamed(dirOf(ev), ev.getOldName(), ev.getBClass().getName());
        } catch (Exception e) {
            log("classNameChanged", e);
        }
    }

        /**
     * Reports a removal. If BlueJ's class object can no longer give its name, requests a folder
     * scan instead.
     *
     * @param ev BlueJ's class notification
     */
    @Override
    public void classRemoved(ClassEvent ev) {
        Path dir = null;
        try {
            dir = dirOf(ev);
            tracker.classRemoved(dir, ev.getBClass().getName());
        } catch (Exception e) {
            if (dir != null) {
                watcher.requestReconcile(dir);
            } else {
                log("classRemoved", e);
            }
        }
    }

    // helpers

        /**
     * Returns the folder of the event's package.
     *
     * @param ev BlueJ's class notification
     * @return the folder of the event's package
     */
    private static Path dirOf(ClassEvent ev) throws Exception {
        return ev.getPackage().getDir().toPath();
    }

        /**
     * Prints an error to BlueJ's debug log.
     *
     * @param where where the error happened
     * @param e the event
     */
    private static void log(String where, Exception e) {
        System.err.println("[telemetry] " + where + " failed: " + e); // goes to BlueJ's debug log
    }
}
