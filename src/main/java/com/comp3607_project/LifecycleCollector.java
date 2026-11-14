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


public final class LifecycleCollector implements PackageListener, ClassListener {

    private final BlueJ bluej;
    private final ProjectTracker tracker;
    private final ProjectWatcher watcher;

    public LifecycleCollector(BlueJ bluej, LifecycleSink sink) throws IOException {
        this.bluej = bluej;
        this.tracker = new ProjectTracker(sink);
        this.watcher = new ProjectWatcher(tracker, ProjectWatcher.DEFAULT_DEBOUNCE_MILLIS);
    }

    // Call from Extension.startup(BlueJ).
    public void register() {
        watcher.start();
        bluej.addPackageListener(this);
        bluej.addClassListener(this);
    }

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

    @Override
    public void classNameChanged(ClassEvent ev) {
        try {
            tracker.classRenamed(dirOf(ev), ev.getOldName(), ev.getBClass().getName());
        } catch (Exception e) {
            log("classNameChanged", e);
        }
    }

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

    private static Path dirOf(ClassEvent ev) throws Exception {
        return ev.getPackage().getDir().toPath();
    }

    private static void log(String where, Exception e) {
        System.err.println("[telemetry] " + where + " failed: " + e); // goes to BlueJ's debug log
    }
}
