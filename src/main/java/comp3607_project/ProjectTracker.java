package comp3607_project;

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

    private final LifecycleSink sink;
    private final Clock clock;
    private final Map<Path, ProjectState> projects = new HashMap<>();
    private final Map<Path, PackageState> packages = new HashMap<>();

    public ProjectTracker(LifecycleSink sink) {
        this(sink, Clock.systemUTC());
    }

    public ProjectTracker(LifecycleSink sink, Clock clock) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // project level

    
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

    public synchronized boolean isTrackingPackage(Path packageDir) {
        return packages.containsKey(norm(packageDir));
    }

    public synchronized boolean isTrackingProject(Path projectDir) {
        return projects.containsKey(norm(projectDir));
    }

    public synchronized Set<String> knownClasses(Path packageDir) {
        PackageState pkg = packages.get(norm(packageDir));
        return pkg == null ? Set.of() : Set.copyOf(pkg.classes);
    }

    // internals

    // A class-level change is also a modification of the project that owns the class.
    private void classChange(LifecycleEventType type, ProjectState project, PackageState pkg,
                             String className, String previousClassName, String detail) {
        record(type, project, pkg.packageName, className, previousClassName, detail);
        String cause = previousClassName == null
                ? type.name() + " " + className
                : type.name() + " " + previousClassName + " -> " + className;
        record(LifecycleEventType.PROJECT_MODIFIED, project, pkg.packageName, null, null, cause);
    }

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

    private static Path norm(Path p) {
        return p.toAbsolutePath().normalize();
    }
}
