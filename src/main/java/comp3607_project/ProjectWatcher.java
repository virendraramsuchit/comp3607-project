package com.comp3607_project;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public final class ProjectWatcher implements AutoCloseable {

    public static final long DEFAULT_DEBOUNCE_MILLIS = 500;
    public static final long EXISTENCE_CHECK_MILLIS = 1000;
    private static final long POLL_MILLIS = 100;

    private final WatchService watchService;
    private final ProjectTracker tracker;
    private final long debounceMillis;
    private final Map<WatchKey, Path> keyToDir = new ConcurrentHashMap<>();
    private final Map<Path, WatchKey> dirToKey = new ConcurrentHashMap<>();
    // The directory which is thetime of the most recent relevant change.
    private final Map<Path, Long> dirty = new ConcurrentHashMap<>();
    // Folders whose watch was dropped and that may have been deleted.
    private final Set<Path> suspects = ConcurrentHashMap.newKeySet();
    private final Thread thread;
    private final AtomicBoolean started = new AtomicBoolean();
    private volatile boolean running = true;
    private long lastExistenceCheck;

    public ProjectWatcher(ProjectTracker tracker, long debounceMillis) throws IOException {
        this.tracker = tracker;
        this.debounceMillis = debounceMillis;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.thread = new Thread(this::runLoop, "telemetry-lifecycle-watcher");
        this.thread.setDaemon(true);
    }

    // Starts the background thread.
    public void start() {
        if (started.compareAndSet(false, true)) {
            thread.start();
        }
    }

    public boolean isRunning() {
        return thread.isAlive();
    }

    public boolean isWatching(Path dir) {
        return dirToKey.containsKey(norm(dir));
    }

    // Starts watching a package folder and schedules a scan of it.
    public void watch(Path dir) {
        Path d = norm(dir);
        if (dirToKey.containsKey(d)) {
            return;
        }
        try {
            WatchKey key = d.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            keyToDir.put(key, d);
            dirToKey.put(d, key);
            requestReconcile(d);
        } catch (IOException | RuntimeException e) {
            System.err.println("[telemetry] cannot watch " + d + ": " + e);
        }
    }

    public void unwatch(Path dir) {
        Path d = norm(dir);
        WatchKey key = dirToKey.remove(d);
        if (key != null) {
            keyToDir.remove(key);
            key.cancel();
        }
        dirty.remove(d);
        suspects.remove(d);
    }

    // Schedules a debounced folder scan.
    public void requestReconcile(Path dir) {
        dirty.put(norm(dir), System.currentTimeMillis());
    }

    @Override
    public void close() {
        running = false;
        thread.interrupt();
        try {
            watchService.close();
        } catch (IOException ignored) {
            // nothing useful to do while shutting down
        }
    }

    // watcher thread

    private void runLoop() {
        while (running) {
            try {
                WatchKey key = watchService.poll(POLL_MILLIS, TimeUnit.MILLISECONDS);
                if (key != null) {
                    handle(key);
                }
                flushDue();
                checkStillExists();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            } catch (RuntimeException e) {
                System.err.println("[telemetry] watcher error: " + e); // keep watching
            }
        }
    }

    private void handle(WatchKey key) {
        Path dir = keyToDir.get(key);
        boolean relevant = false;
        for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                relevant = true; // events were lost
                continue;
            }
            Object ctx = event.context();
            if (ctx instanceof Path && SourceFiles.baseName(((Path) ctx).getFileName().toString()) != null) {
                relevant = true;
            }
        }
        if (dir != null && relevant) {
            dirty.put(dir, System.currentTimeMillis());
        }
        if (!key.reset() && dir != null) {

            keyToDir.remove(key);
            dirToKey.remove(dir);
            dirty.remove(dir);
            suspects.add(dir);
        }
    }

    private void flushDue() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<Path, Long>> it = dirty.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Path, Long> entry = it.next();
            if (now - entry.getValue() >= debounceMillis) {
                it.remove();
                scan(entry.getKey());
            }
        }
    }

    private void checkStillExists() {
        long now = System.currentTimeMillis();
        if (now - lastExistenceCheck < EXISTENCE_CHECK_MILLIS) {
            return;
        }
        lastExistenceCheck = now;
        for (Path dir : new ArrayList<>(dirToKey.keySet())) {
            if (Files.notExists(dir)) {
                gone(dir);
            }
        }
        for (Path dir : new ArrayList<>(suspects)) {
            if (Files.notExists(dir)) {
                gone(dir);
            } else if (Files.isDirectory(dir)) {
                suspects.remove(dir);
                watch(dir);
            }
           
        }
    }

    private void scan(Path dir) {
        try {
            tracker.reconcile(dir, SourceFiles.snapshot(dir));
        } catch (IOException e) {
            if (Files.notExists(dir)) {
                gone(dir);
            }
        }
    }

    // Only called once the folder is confirmed missing.
    private void gone(Path dir) {
        unwatch(dir);
        tracker.directoryDeleted(dir);
    }

    private static Path norm(Path p) {
        return p.toAbsolutePath().normalize();
    }
}
