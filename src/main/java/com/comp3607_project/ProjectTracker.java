package com.comp3607_project;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The single source of truth for which projects, packages and classes are open. It combines
 * what BlueJ reports with what folder scans find, without reporting anything twice, and
 * creates {@link LifecycleEvent}s. Every public method is {@code synchronized}, because it is
 * called from BlueJ's threads and from the watcher thread.
 */
public final class ProjectTracker {

    private static final class ProjectState {
        final String name;
        final Path dir;
        final long openedAt;
        final Set<Path> openPackages = new HashSet<>();

        ProjectState(String name, Path dir, long openedAt) {
            this.name = name;
            this.dir = dir;
            this.openedAt = openedAt;
        }
    }

    private static final class PackageState {
        final Path dir;
        final Path projectDir;
        final String packageName;
        // Single source of truth for which classes exist (de-duplication).
        final Set<String> classes = new HashSet<>();
        // Last known content fingerprint per class source file.
        final Map<String, String> fingerprints = new HashMap<>();
        // Source files seen on disk by the previous scan.
        Set<String> lastScan = new HashSet<>();

        PackageState(Path dir, Path projectDir, String packageName) {
            this.dir = dir;
            this.projectDir = projectDir;
            this.packageName = packageName;
        }
    }

        /**
     * Where lifecycle events are sent.
     */
    private final LifecycleSink sink;

        /**
     * Source of time, so tests can control it.
     */
    private final Clock clock;

        /**
     * Open projects by folder.
     */
    private final Map<Path, ProjectState> projects = new HashMap<>();

        /**
     * Open packages by folder.
     */
    private final Map<Path, PackageState> packages = new HashMap<>();

        /**
     * Uses the system UTC clock.
     *
     * @param sink where events are sent
     */
    public ProjectTracker(LifecycleSink sink) {
        this(sink, Clock.systemUTC());
    }

        /**
     * Uses the given clock.
     *
     * @param sink where events are sent
     * @param clock the clock used for event times
     * @throws NullPointerException if either argument is {@code null}
     */
    public ProjectTracker(LifecycleSink sink, Clock clock) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // project level

   
        /**
     * Starts tracking a package. For the first package of a project, records {@code
     * PROJECT_CREATED} (if {@code looksNewProject}) and {@code PROJECT_OPENED}. A package already
     * tracked is ignored.
     *
     * @param packageDir the package folder
     * @param projectDir the project folder
     * @param projectName the project name
     * @param packageName the package name ({@code ""} for the default package)
     * @param bluejClasses class names reported by BlueJ
     * @param sourcesOnDisk class name to content fingerprint, from a folder scan
     * @param looksNewProject true if the project appears to have just been created
     */
    public synchronized void packageOpened(Path packageDir, Path projectDir, String projectName,
                                           String packageName, Collection<String> bluejClasses,
                                           Map<String, String> sourcesOnDisk,
                                           boolean looksNewProject) {
        Path pkgKey = norm(packageDir);
        Path projKey = norm(projectDir);
        if (packages.containsKey(pkgKey)) {
            return; // already tracking this package
        }
        String pkgName = packageName == null ? "" : packageName;
        PackageState pkg = new PackageState(pkgKey, projKey, pkgName);
        for (String c : bluejClasses) {
            pkg.classes.add(SourceFiles.simpleName(c));
        }
        for (Map.Entry<String, String> e : sourcesOnDisk.entrySet()) {
            pkg.classes.add(e.getKey());
            pkg.lastScan.add(e.getKey());
            if (e.getValue() != null) {
                pkg.fingerprints.put(e.getKey(), e.getValue());
            }
        }

        ProjectState project = projects.get(projKey);
        if (project == null) {
            project = new ProjectState(projectName, projKey, clock.millis());
            projects.put(projKey, project);
            if (looksNewProject) {
                record(LifecycleEventType.PROJECT_CREATED, project, pkgName, null, null,
                        "inferred: empty project whose package.bluej was just written");
            }
            record(LifecycleEventType.PROJECT_OPENED, project, pkgName, null, null,
                    "existingClasses=" + pkg.classes.size());
        }
        project.openPackages.add(pkgKey);
        packages.put(pkgKey, pkg);
    }

        /**
     * Stops tracking a package. When the project's last package closes, records {@code
     * PROJECT_CLOSED} with how long it was open.
     *
     * @param packageDir the package folder
     */
    public synchronized void packageClosing(Path packageDir) {
        PackageState pkg = packages.remove(norm(packageDir));
        if (pkg == null) {
            return;
        }
        ProjectState project = projects.get(pkg.projectDir);
        if (project == null) {
            return;
        }
        project.openPackages.remove(pkg.dir);
        if (project.openPackages.isEmpty()) {
            projects.remove(project.dir);
            record(LifecycleEventType.PROJECT_CLOSED, project, pkg.packageName, null, null,
                    "openDurationMillis=" + (clock.millis() - project.openedAt));
        }
    }

        /**
     * For a project folder, records {@code PROJECT_DELETED}. For a package folder, records {@code
     * CLASS_DELETED} for each of its classes.
     *
     * @param dir a package or project folder
     */
    // Called when a watched folder no longer exists on disk.
    public synchronized void directoryDeleted(Path dir) {
        Path d = norm(dir);
        ProjectState project = projects.remove(d);
        if (project != null) {
            for (Path p : project.openPackages) {
                packages.remove(p);
            }
            record(LifecycleEventType.PROJECT_DELETED, project, "", null, null,
                    "project directory no longer exists on disk");
            return;
        }
      
        PackageState pkg = packages.remove(d);
        if (pkg == null) {
            return;
        }
        ProjectState owner = projects.get(pkg.projectDir);
        if (owner != null) {
            owner.openPackages.remove(d);
        }
        for (String name : new TreeSet<>(pkg.classes)) {
            classChange(LifecycleEventType.CLASS_DELETED, owner, pkg, name, null,
                    "package directory deleted");
        }
    }

        /**
     * Records {@code PROJECT_CLOSED} for every open project and clears all state.
     *
     * @param reason why the session is ending, written into the event detail
     */
    public synchronized void shutdown(String reason) {
        long now = clock.millis();
        for (ProjectState project : new ArrayList<>(projects.values())) {
            record(LifecycleEventType.PROJECT_CLOSED, project, "", null, null,
                    "openDurationMillis=" + (now - project.openedAt) + "; reason=" + reason);
        }
        projects.clear();
        packages.clear();
    }

    // class level (BlueJ)

        /**
     * Records {@code CLASS_CREATED} the first time a class is reported.
     *
     * @param packageDir the package folder
     * @param className the class name
     */
    // BlueJ reported a class . Reports CLASS_CREATED the first time.
    public synchronized void classSeen(Path packageDir, String className) {
        PackageState pkg = packages.get(norm(packageDir));
        if (pkg == null) {
            return;
        }
        String name = SourceFiles.simpleName(className);
        if (pkg.classes.add(name)) {
            classChange(LifecycleEventType.CLASS_CREATED, projects.get(pkg.projectDir), pkg, name, null,
                    "reported by BlueJ");
        }
    }

        /**
     * Records {@code CLASS_RENAMED} (ignored if the name is unchanged).
     *
     * @param packageDir the package folder
     * @param oldName the class name before the rename
     * @param newName the class name after the rename
     */
    public synchronized void classRenamed(Path packageDir, String oldName, String newName) {
        PackageState pkg = packages.get(norm(packageDir));
        if (pkg == null) {
            return;
        }
        String from = SourceFiles.simpleName(oldName);
        String to = SourceFiles.simpleName(newName);
        if (from.equals(to)) {
            return;
        }
        pkg.classes.remove(from);
        pkg.classes.add(to);
        pkg.fingerprints.remove(from);
        pkg.fingerprints.remove(to);
        classChange(LifecycleEventType.CLASS_RENAMED, projects.get(pkg.projectDir), pkg, to, from, null);
    }

        /**
     * Records {@code CLASS_DELETED} for a known class.
     *
     * @param packageDir the package folder
     * @param className the class name
     */
    public synchronized void classRemoved(Path packageDir, String className) {
        PackageState pkg = packages.get(norm(packageDir));
        if (pkg == null) {
            return;
        }
        String name = SourceFiles.simpleName(className);
        if (pkg.classes.remove(name)) {
            pkg.fingerprints.remove(name);
            classChange(LifecycleEventType.CLASS_DELETED, projects.get(pkg.projectDir), pkg, name, null,
                    "removed via BlueJ");
        }
    }

    // class level (disk)

        /**
     * Compares a folder scan (class name -> fingerprint) with what is known. Records classes
     * created or deleted outside BlueJ, and {@code CLASS_MODIFIED} when a file's content
     * fingerprint changes.
     *
     * @param packageDir the package folder
     * @param onDisk class name to content fingerprint (null if unreadable) from a folder scan
     */
    // Compares a fresh folder scan (class name -> fingerprint) with what is known.
    // Reports classes created or deleted outside BlueJ and classes whose content changed.
    public synchronized void reconcile(Path packageDir, Map<String, String> onDisk) {
        PackageState pkg = packages.get(norm(packageDir));
        if (pkg == null) {
            return;
        }
        ProjectState project = projects.get(pkg.projectDir);
        Set<String> previous = pkg.lastScan;
        pkg.lastScan = new HashSet<>(onDisk.keySet());

        for (String name : new TreeSet<>(onDisk.keySet())) {
            String fingerprint = onDisk.get(name);
            if (!previous.contains(name)) {
                if (fingerprint != null) {
                    pkg.fingerprints.put(name, fingerprint);
                }
                if (pkg.classes.add(name)) {
                    classChange(LifecycleEventType.CLASS_CREATED, project, pkg, name, null,
                            "detected on disk");
                }
                continue;
            }
            if (fingerprint == null) {
                continue; 
            }
            String old = pkg.fingerprints.put(name, fingerprint);
            if (old != null && !old.equals(fingerprint) && pkg.classes.contains(name)) {
                classChange(LifecycleEventType.CLASS_MODIFIED, project, pkg, name, null,
                        "source content changed");
            }
        }

        Set<String> vanished = new TreeSet<>(previous);
        vanished.removeAll(onDisk.keySet());
        for (String name : vanished) {
            pkg.fingerprints.remove(name);
            if (pkg.classes.remove(name)) {
                classChange(LifecycleEventType.CLASS_DELETED, project, pkg, name, null,
                        "detected on disk");
            }
        }
    }

    // queries (for tests)

        /**
     * Returns {@code true} if the package is open.
     *
     * @param packageDir the package folder
     * @return {@code true} if the package is open
     */
    public synchronized boolean isTrackingPackage(Path packageDir) {
        return packages.containsKey(norm(packageDir));
    }

        /**
     * Returns {@code true} if the project is open.
     *
     * @param projectDir the project folder
     * @return {@code true} if the project is open
     */
    public synchronized boolean isTrackingProject(Path projectDir) {
        return projects.containsKey(norm(projectDir));
    }

        /**
     * Returns a copy of the package's known classes (empty if not tracked).
     *
     * @param packageDir the package folder
     * @return a copy of the package's known classes
     */
    public synchronized Set<String> knownClasses(Path packageDir) {
        PackageState pkg = packages.get(norm(packageDir));
        return pkg == null ? Set.of() : Set.copyOf(pkg.classes);
    }

    // internals

        /**
     * Records a class event and the matching {@code PROJECT_MODIFIED}.
     *
     * @param type the lifecycle event type
     * @param project the project the change belongs to
     * @param pkg the package the change belongs to
     * @param className the class name
     * @param previousClassName the old class name (renames only)
     * @param detail extra information, may be null
     */
    // A class-level change is also a modification of the project that owns the class.
    private void classChange(LifecycleEventType type, ProjectState project, PackageState pkg,
                             String className, String previousClassName, String detail) {
        record(type, project, pkg.packageName, className, previousClassName, detail);
        String cause = previousClassName == null
                ? type.name() + " " + className
                : type.name() + " " + previousClassName + " -> " + className;
        record(LifecycleEventType.PROJECT_MODIFIED, project, pkg.packageName, null, null, cause);
    }

        /**
     * Creates a {@code LifecycleEvent} and sends it to the sink. A sink error is printed and never
     * thrown.
     *
     * @param type the lifecycle event type
     * @param project the project the change belongs to
     * @param packageName the package name ({@code ""} for the default package)
     * @param className the class name
     * @param previousClassName the old class name (renames only)
     * @param detail extra information, may be null
     */
    private void record(LifecycleEventType type, ProjectState project, String packageName,
                        String className, String previousClassName, String detail) {
        LifecycleEvent event = new LifecycleEvent(clock.millis(), type,
                project == null ? null : project.name,
                project == null ? null : project.dir.toString(),
                packageName, className, previousClassName, detail);
        try {
            sink.accept(event);
        } catch (RuntimeException e) {
            // A faulty sink must never break BlueJ or the other listeners.
            System.err.println("[telemetry] sink rejected " + type + ": " + e);
        }
    }

        /**
     * Returns the absolute, normalised path.
     *
     * @param p the path to normalise
     * @return the absolute, normalised path
     */
    private static Path norm(Path p) {
        return p.toAbsolutePath().normalize();
    }
}
