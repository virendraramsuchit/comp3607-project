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

/**
 * Watches open package folders with Java's {@code WatchService} on a background daemon thread.
 * It catches classes created, edited or deleted outside BlueJ, and project folders that are
 * deleted or moved. Changes are debounced: a folder is scanned once it has been quiet for the
 * debounce time.
 */
public final class ProjectWatcher implements AutoCloseable {

        /**
     * Default quiet time before a scan: {@code 500} ms.
     */
    public static final long DEFAULT_DEBOUNCE_MILLIS = 500;

        /**
     * How often watched folders are checked for deletion: {@code 1000} ms.
     */
    public static final long EXISTENCE_CHECK_MILLIS = 1000;

        /**
     * How long the thread waits for file events per loop: {@code 100} ms.
     */
    private static final long POLL_MILLIS = 100;

        /**
     * Delivers file-system events.
     */
    private final WatchService watchService;

        /**
     * Receives scan results and deletions.
     */
    private final ProjectTracker tracker;

        /**
     * Quiet time before a scan.
     */
    private final long debounceMillis;

        /**
     * Folder for each watch key.
     */
    private final Map<WatchKey, Path> keyToDir = new ConcurrentHashMap<>();

        /**
     * Watch key for each folder.
     */
    private final Map<Path, WatchKey> dirToKey = new ConcurrentHashMap<>();

        /**
     * Folders waiting to be scanned, with the time of their latest change.
     */
    // The directory which is thetime of the most recent relevant change.
    private final Map<Path, Long> dirty = new ConcurrentHashMap<>();

        /**
     * Folders whose watch was dropped and that may have been deleted.
     */
    // Folders whose watch was dropped and that may have been deleted.
    private final Set<Path> suspects = ConcurrentHashMap.newKeySet();

        /**
     * The background thread, named {@code telemetry-lifecycle-watcher}.
     */
    private final Thread thread;

        /**
     * Makes sure the thread is started only once.
     */
    private final AtomicBoolean started = new AtomicBoolean();

        /**
     * *volatile.* Set to {@code false} to stop the loop.
     */
    private volatile boolean running = true;

        /**
     * Time of the last deletion check.
     */
    private long lastExistenceCheck;

        /**
     * Creates the watch service and the (not yet started) thread.
     *
     * @param tracker the tracker that receives the data
     * @param debounceMillis how long a folder must be quiet before it is scanned
     * @throws IOException if the watch service cannot be created
     */
    public ProjectWatcher(ProjectTracker tracker, long debounceMillis) throws IOException {
        this.tracker = tracker;
        this.debounceMillis = debounceMillis;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.thread = new Thread(this::runLoop, "telemetry-lifecycle-watcher");
        this.thread.setDaemon(true);
    }

        /**
     * Starts the background thread; further calls do nothing.
     */
    // Starts the background thread.
    public void start() {
        if (started.compareAndSet(false, true)) {
            thread.start();
        }
    }

        /**
     * Returns {@code true} while the thread is alive.
     * @return {@code true} while the thread is alive
     */
    public boolean isRunning() {
        return thread.isAlive();
    }

        /**
     * Returns {@code true} if the folder is being watched.
     *
     * @param dir a package or project folder
     * @return {@code true} if the folder is being watched
     */
    public boolean isWatching(Path dir) {
        return dirToKey.containsKey(norm(dir));
    }

        /**
     * Starts watching a folder for creates, deletes and edits, and schedules a scan.
     * Already-watched folders are ignored; errors are logged.
     *
     * @param dir a package or project folder
     */
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

        /**
     * Stops watching a folder and forgets any pending scan.
     *
     * @param dir a package or project folder
     */
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

        /**
     * Schedules a debounced scan of the folder.
     *
     * @param dir a package or project folder
     */
    // Schedules a debounced folder scan.
    public void requestReconcile(Path dir) {
        dirty.put(norm(dir), System.currentTimeMillis());
    }

        /**
     * Stops the thread and closes the watch service.
     */
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

        /**
     * The thread's loop: wait for file events, scan quiet folders, check for deleted folders.
     */
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

        /**
     * Marks the folder for scanning if a {@code .java} or {@code .stride} file changed (or events
     * were lost). If the watch became invalid, marks the folder as a suspect.
     *
     * @param key the watch key that has events
     */
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

        /**
     * Scans every folder that has been quiet for the debounce time.
     */
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

        /**
     * Once a second, reports watched or suspect folders that no longer exist, and re-watches
     * suspects that still exist.
     */
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

        /**
     * Takes a snapshot of the folder and passes it to {@code tracker.reconcile}.
     *
     * @param dir a package or project folder
     */
    private void scan(Path dir) {
        try {
            tracker.reconcile(dir, SourceFiles.snapshot(dir));
        } catch (IOException e) {
            if (Files.notExists(dir)) {
                gone(dir);
            }
        }
    }

        /**
     * Stops watching a confirmed-missing folder and calls {@code tracker.directoryDeleted}.
     *
     * @param dir a package or project folder
     */
    // Only called once the folder is confirmed missing.
    private void gone(Path dir) {
        unwatch(dir);
        tracker.directoryDeleted(dir);
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
