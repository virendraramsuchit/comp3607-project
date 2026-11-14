package comp3607_project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import comp3607_project.LifecycleEventType;
import comp3607_project.ProjectTracker;
import comp3607_project.ProjectWatcher;
import comp3607_project.SourceFiles;

class ProjectWatcherTest {

    private static final long DEBOUNCE = 50;
    private static final long TIMEOUT_MILLIS = 15_000;

    @TempDir
    Path root;

    private Path project;
    private RecordingSink sink;
    private ProjectTracker tracker;
    private ProjectWatcher watcher;

    @BeforeEach
    void setUp() throws IOException {
        project = Files.createDirectory(root.resolve("Shapes"));
        Files.writeString(project.resolve("A.java"), "class A {}");
        sink = new RecordingSink();
        tracker = new ProjectTracker(sink);
        watcher = new ProjectWatcher(tracker, DEBOUNCE);
        tracker.packageOpened(project, project, "Shapes", "", List.of("A"),
                SourceFiles.snapshot(project), false);
        watcher.watch(project);
        watcher.start();
        sink.clear();
    }

    @AfterEach
    void tearDown() {
        watcher.close();
    }

    private void await(String what, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("timed out waiting for " + what + "; events were " + sink.types());
    }

    private boolean saw(LifecycleEventType type, String className) {
        return sink.events().stream().anyMatch(e -> e.getType() == type
                && (className == null || className.equals(e.getClassName())));
    }

    @Test
    void startsBackgroundThreadAndToleratesSecondStart() {
        watcher.start();
        assertTrue(watcher.isRunning());
        assertTrue(watcher.isWatching(project));
    }

    @Test
    void detectsClassCreatedOnDisk() throws Exception {
        Files.writeString(project.resolve("B.java"), "class B {}");

        await("CLASS_CREATED B", () -> saw(LifecycleEventType.CLASS_CREATED, "B"));
        await("PROJECT_MODIFIED", () -> saw(LifecycleEventType.PROJECT_MODIFIED, null));
    }

    @Test
    void detectsSavedEdit() throws Exception {
        Files.writeString(project.resolve("A.java"), "class A { int x; }");

        await("CLASS_MODIFIED A", () -> saw(LifecycleEventType.CLASS_MODIFIED, "A"));
    }

    @Test
    void detectsClassDeletedOnDisk() throws Exception {
        Files.delete(project.resolve("A.java"));

        await("CLASS_DELETED A", () -> saw(LifecycleEventType.CLASS_DELETED, "A"));
        assertFalse(tracker.knownClasses(project).contains("A"));
    }

    @Test
    void ignoresNonSourceFiles() throws Exception {
        Files.writeString(project.resolve("A.class"), "binary");
        Files.writeString(project.resolve("README.TXT"), "notes");
        // Written elsewhere then moved in, so the scan never sees a half-written file.
        Path tmp = Files.writeString(root.resolve("Marker.tmp"), "class Marker {}");
        Files.move(tmp, project.resolve("Marker.java"), StandardCopyOption.ATOMIC_MOVE);

        // Once the marker is seen, any event for the other files would have been seen too.
        await("CLASS_CREATED Marker", () -> saw(LifecycleEventType.CLASS_CREATED, "Marker"));
        assertEquals(List.of(LifecycleEventType.CLASS_CREATED, LifecycleEventType.PROJECT_MODIFIED),
                sink.types());
    }

    @Test
    void detectsProjectFolderDeleted() throws Exception {
        deleteTree(project);

        await("PROJECT_DELETED", () -> saw(LifecycleEventType.PROJECT_DELETED, null));
        await("folder no longer watched", () -> !watcher.isWatching(project));
        assertFalse(tracker.isTrackingProject(project));
    }

    @Test
    void detectsProjectFolderMovedAway() throws Exception {
        // Windows "delete to Recycle Bin" is really a move.
        Files.move(project, root.resolve("RecycleBin-Shapes"));

        await("PROJECT_DELETED", () -> saw(LifecycleEventType.PROJECT_DELETED, null));
    }

    @Test
    void requestReconcileRescansWithoutNotification() throws Exception {
        watcher.unwatch(project); // no notifications from now on
        Files.writeString(project.resolve("A.java"), "class A { void m() {} }");

        watcher.requestReconcile(project);

        await("CLASS_MODIFIED A", () -> saw(LifecycleEventType.CLASS_MODIFIED, "A"));
    }

    @Test
    void unwatchStopsReporting() throws Exception {
        watcher.unwatch(project);
        assertFalse(watcher.isWatching(project));
        Files.writeString(project.resolve("B.java"), "class B {}");

        Thread.sleep(DEBOUNCE * 10);
        assertTrue(sink.types().isEmpty());
    }

    @Test
    void watchingSameFolderTwiceIsHarmless() throws Exception {
        watcher.watch(project);
        Files.writeString(project.resolve("B.java"), "class B {}");

        await("CLASS_CREATED B", () -> saw(LifecycleEventType.CLASS_CREATED, "B"));
        assertEquals(1, sink.events().stream()
                .filter(e -> e.getType() == LifecycleEventType.CLASS_CREATED).count());
    }

    @Test
    void watchingMissingFolderDoesNotThrow() {
        Path missing = root.resolve("nope");
        watcher.watch(missing);
        assertFalse(watcher.isWatching(missing));
    }

    @Test
    void closeStopsThread() throws Exception {
        watcher.close();
        await("thread stopped", () -> !watcher.isRunning());
    }

    private static void deleteTree(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) walk.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(p);
            }
        }
    }
}
