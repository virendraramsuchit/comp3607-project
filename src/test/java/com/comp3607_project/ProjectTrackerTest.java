package com.comp3607_project;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.comp3607_project.LifecycleEventType.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp3607_project.LifecycleEvent;
import com.comp3607_project.ProjectTracker;

class ProjectTrackerTest {

    private static final Path PROJECT = Path.of("work", "Shapes");
    private static final Path SUB = PROJECT.resolve("geo");

    private RecordingSink sink;
    private MutableClock clock;
    private ProjectTracker tracker;

    @BeforeEach
    void setUp() {
        sink = new RecordingSink();
        clock = new MutableClock(1_000_000L);
        tracker = new ProjectTracker(sink, clock);
    }

    // Opens the default package with the given classes on disk 
    private void openDefault(Map<String, String> onDisk) {
        tracker.packageOpened(PROJECT, PROJECT, "Shapes", "", onDisk.keySet(), onDisk, false);
    }

    private static Map<String, String> disk(String... nameThenFingerprint) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < nameThenFingerprint.length; i += 2) {
            m.put(nameThenFingerprint[i], nameThenFingerprint[i + 1]);
        }
        return m;
    }

    // projects

    @Test
    void openingAProjectReportsOpenedWithClassCount() {
        openDefault(disk("A", "h1", "B", "h2"));

        assertEquals(List.of(PROJECT_OPENED), sink.types());
        LifecycleEvent e = sink.last();
        assertEquals("Shapes", e.getProjectName());
        assertEquals(PROJECT.toAbsolutePath().normalize().toString(), e.getProjectPath());
        assertEquals("existingClasses=2", e.getDetail());
        assertEquals(1_000_000L, e.getEpochMillis());
        assertTrue(tracker.isTrackingProject(PROJECT));
        assertTrue(tracker.isTrackingPackage(PROJECT));
    }

    @Test
    void newProjectReportsCreatedThenOpened() {
        tracker.packageOpened(PROJECT, PROJECT, "Shapes", "", List.of(), Map.of(), true);

        assertEquals(List.of(PROJECT_CREATED, PROJECT_OPENED), sink.types());
    }

    @Test
    void classesFromBlueJAndDiskAreMergedWithoutDuplicates() {
        tracker.packageOpened(PROJECT, PROJECT, "Shapes", "", List.of("A", "pkg.B"),
                disk("A", "h1", "C", "h3"), false);

        assertEquals(Set.of("A", "B", "C"), tracker.knownClasses(PROJECT));
        assertEquals("existingClasses=3", sink.last().getDetail());
    }

    @Test
    void openingTheSamePackageTwiceIsIgnored() {
        openDefault(Map.of());
        openDefault(Map.of());

        assertEquals(List.of(PROJECT_OPENED), sink.types());
    }

    @Test
    void secondPackageOfAnOpenProjectReportsNothing() {
        openDefault(Map.of());
        tracker.packageOpened(SUB, PROJECT, "Shapes", "geo", List.of(), Map.of(), false);

        assertEquals(List.of(PROJECT_OPENED), sink.types());
        assertTrue(tracker.isTrackingPackage(SUB));
    }

    @Test
    void nullPackageNameIsTreatedAsDefaultPackage() {
        tracker.packageOpened(PROJECT, PROJECT, "Shapes", null, List.of(), Map.of(), false);

        assertEquals("", sink.last().getPackageName());
    }

    @Test
    void closingLastPackageReportsClosedWithDuration() {
        openDefault(Map.of());
        clock.advance(5_000);
        tracker.packageClosing(PROJECT);

        assertEquals(List.of(PROJECT_OPENED, PROJECT_CLOSED), sink.types());
        assertEquals("openDurationMillis=5000", sink.last().getDetail());
        assertFalse(tracker.isTrackingProject(PROJECT));
    }

    @Test
    void projectStaysOpenUntilAllItsPackagesClose() {
        openDefault(Map.of());
        tracker.packageOpened(SUB, PROJECT, "Shapes", "geo", List.of(), Map.of(), false);

        tracker.packageClosing(SUB);
        assertEquals(List.of(PROJECT_OPENED), sink.types());

        tracker.packageClosing(PROJECT);
        assertEquals(List.of(PROJECT_OPENED, PROJECT_CLOSED), sink.types());
    }

    @Test
    void closingAnUnknownPackageIsIgnored() {
        tracker.packageClosing(PROJECT);
        assertTrue(sink.types().isEmpty());
    }

    @Test
    void deletingTheProjectFolderReportsDeletedAndNotClosedLater() {
        openDefault(disk("A", "h1"));
        tracker.packageOpened(SUB, PROJECT, "Shapes", "geo", List.of(), Map.of(), false);

        tracker.directoryDeleted(PROJECT);
        tracker.shutdown("session-end");
        tracker.packageClosing(PROJECT);

        assertEquals(List.of(PROJECT_OPENED, PROJECT_DELETED), sink.types());
        assertFalse(tracker.isTrackingPackage(PROJECT));
        assertFalse(tracker.isTrackingPackage(SUB));
    }

    @Test
    void projectDeletedIsReportedOnlyOnce() {
        openDefault(Map.of());
        tracker.directoryDeleted(PROJECT);
        tracker.directoryDeleted(PROJECT);

        assertEquals(List.of(PROJECT_OPENED, PROJECT_DELETED), sink.types());
    }

    @Test
    void deletingASubPackageFolderDeletesItsClasses() {
        openDefault(Map.of());
        tracker.packageOpened(SUB, PROJECT, "Shapes", "geo", List.of(), disk("Circle", "h1", "Box", "h2"), false);
        sink.clear();

        tracker.directoryDeleted(SUB);

        assertEquals(List.of(CLASS_DELETED, PROJECT_MODIFIED, CLASS_DELETED, PROJECT_MODIFIED), sink.types());
        assertEquals("Box", sink.events().get(0).getClassName());
        assertEquals("Circle", sink.events().get(2).getClassName());
        assertEquals("geo", sink.events().get(0).getPackageName());
        assertTrue(tracker.isTrackingProject(PROJECT));
        assertFalse(tracker.isTrackingPackage(SUB));

        // The project is still open; closing its last package now closes it.
        tracker.packageClosing(PROJECT);
        assertEquals(PROJECT_CLOSED, sink.last().getType());
    }

    @Test
    void deletingAnUnknownFolderIsIgnored() {
        tracker.directoryDeleted(PROJECT);
        assertTrue(sink.types().isEmpty());
    }

    @Test
    void shutdownClosesEveryOpenProject() {
        openDefault(Map.of());
        Path other = Path.of("work", "Bank");
        tracker.packageOpened(other, other, "Bank", "", List.of(), Map.of(), false);
        sink.clear();
        clock.advance(60_000);

        tracker.shutdown("session-end");
        tracker.shutdown("session-end"); // second call reports nothing more

        assertEquals(List.of(PROJECT_CLOSED, PROJECT_CLOSED), sink.types());
        assertEquals("openDurationMillis=60000; reason=session-end", sink.last().getDetail());
    }

    // classes via BlueJ

    @Test
    void newClassFromBlueJReportsClassCreatedAndProjectModified() {
        openDefault(Map.of());
        sink.clear();

        tracker.classSeen(PROJECT, "Circle");

        assertEquals(List.of(CLASS_CREATED, PROJECT_MODIFIED), sink.types());
        assertEquals("Circle", sink.events().get(0).getClassName());
        assertEquals("CLASS_CREATED Circle", sink.last().getDetail());
    }

    @Test
    void knownClassStateChangeReportsNothing() {
        // e.g. BlueJ marks a dependent class "uncompiled" or a class compiles
        openDefault(disk("A", "h1"));
        sink.clear();

        tracker.classSeen(PROJECT, "A");
        tracker.classSeen(PROJECT, "pkg.A");

        assertTrue(sink.types().isEmpty());
    }

    @Test
    void classEventsForUntrackedPackagesAreIgnored() {
        tracker.classSeen(PROJECT, "A");
        tracker.classRenamed(PROJECT, "A", "B");
        tracker.classRemoved(PROJECT, "A");
        tracker.reconcile(PROJECT, disk("A", "h1"));

        assertTrue(sink.types().isEmpty());
        assertEquals(Set.of(), tracker.knownClasses(PROJECT));
    }

    @Test
    void renameReportsClassRenamedAndProjectModified() {
        openDefault(disk("Round", "h1"));
        sink.clear();

        tracker.classRenamed(PROJECT, "Round", "Circle");

        assertEquals(List.of(CLASS_RENAMED, PROJECT_MODIFIED), sink.types());
        LifecycleEvent e = sink.events().get(0);
        assertEquals("Circle", e.getClassName());
        assertEquals("Round", e.getPreviousClassName());
        assertEquals("CLASS_RENAMED Round -> Circle", sink.last().getDetail());
        assertEquals(Set.of("Circle"), tracker.knownClasses(PROJECT));
    }

    @Test
    void renameToSameNameIsIgnored() {
        openDefault(disk("A", "h1"));
        sink.clear();
        tracker.classRenamed(PROJECT, "A", "pkg.A");
        assertTrue(sink.types().isEmpty());
    }

    @Test
    void renameFollowedByFolderScanReportsNothingExtra() {
        openDefault(disk("Round", "h1"));
        tracker.classRenamed(PROJECT, "Round", "Circle");
        sink.clear();

        tracker.reconcile(PROJECT, disk("Circle", "h2"));
        // A later edit to Circle is still detected.
        tracker.reconcile(PROJECT, disk("Circle", "h3"));

        assertEquals(List.of(CLASS_MODIFIED, PROJECT_MODIFIED), sink.types());
    }

    @Test
    void removeViaBlueJReportsDeletedOnce() {
        openDefault(disk("A", "h1"));
        sink.clear();

        tracker.classRemoved(PROJECT, "A");
        tracker.classRemoved(PROJECT, "A");
        tracker.reconcile(PROJECT, Map.of()); // scan sees A.java gone

        assertEquals(List.of(CLASS_DELETED, PROJECT_MODIFIED), sink.types());
        assertEquals("removed via BlueJ", sink.events().get(0).getDetail());
    }

    // classes via folder scan

    @Test
    void sourceContentChangeReportsClassModified() {
        openDefault(disk("A", "h1", "B", "h2"));
        sink.clear();

        tracker.reconcile(PROJECT, disk("A", "h1-edited", "B", "h2"));

        assertEquals(List.of(CLASS_MODIFIED, PROJECT_MODIFIED), sink.types());
        assertEquals("A", sink.events().get(0).getClassName());
        assertEquals("source content changed", sink.events().get(0).getDetail());
    }

    @Test
    void unchangedContentReportsNothing() {
        openDefault(disk("A", "h1"));
        sink.clear();

        tracker.classSeen(PROJECT, "A");
        tracker.reconcile(PROJECT, disk("A", "h1"));

        assertTrue(sink.types().isEmpty());
    }

    @Test
    void everySavedEditIsReported() {
        openDefault(disk("A", "h1"));
        sink.clear();

        tracker.reconcile(PROJECT, disk("A", "h2"));
        tracker.reconcile(PROJECT, disk("A", "h3"));
        tracker.reconcile(PROJECT, disk("A", "h3"));

        assertEquals(List.of(CLASS_MODIFIED, PROJECT_MODIFIED, CLASS_MODIFIED, PROJECT_MODIFIED), sink.types());
    }

    @Test
    void unreadableFileKeepsLastKnownFingerprint() {
        openDefault(disk("A", "h1"));
        sink.clear();

        tracker.reconcile(PROJECT, disk("A", null)); // file locked mid-write
        assertTrue(sink.types().isEmpty());

        tracker.reconcile(PROJECT, disk("A", "h1")); // same content as before
        assertTrue(sink.types().isEmpty());

        tracker.reconcile(PROJECT, disk("A", "h2"));
        assertEquals(List.of(CLASS_MODIFIED, PROJECT_MODIFIED), sink.types());
    }

    @Test
    void fileUnreadableAtOpenGetsFingerprintOnFirstScanWithoutBeingCalledModified() {
        openDefault(disk("A", null));
        sink.clear();

        tracker.reconcile(PROJECT, disk("A", "h1"));
        assertTrue(sink.types().isEmpty());

        tracker.reconcile(PROJECT, disk("A", "h2"));
        assertEquals(List.of(CLASS_MODIFIED, PROJECT_MODIFIED), sink.types());
    }

    @Test
    void fileAddedOutsideBlueJIsReportedCreated() {
        openDefault(disk("A", "h1"));
        sink.clear();

        tracker.reconcile(PROJECT, disk("A", "h1", "B", "h2"));

        assertEquals(List.of(CLASS_CREATED, PROJECT_MODIFIED), sink.types());
        assertEquals("B", sink.events().get(0).getClassName());
        assertEquals("detected on disk", sink.events().get(0).getDetail());
    }

    @Test
    void classCreatedInBlueJIsNotReportedAgainByScan() {
        openDefault(Map.of());
        tracker.classSeen(PROJECT, "B");
        sink.clear();

        tracker.reconcile(PROJECT, disk("B", "template"));      // BlueJ wrote the template
        tracker.reconcile(PROJECT, disk("B", "template+edit")); // then the student edited it

        assertEquals(List.of(CLASS_MODIFIED, PROJECT_MODIFIED), sink.types());
    }

    @Test
    void classFoundOnDiskFirstIsNotReportedAgainByBlueJ() {
        openDefault(Map.of());
        tracker.reconcile(PROJECT, disk("B", "h1"));
        sink.clear();

        tracker.classSeen(PROJECT, "B");

        assertTrue(sink.types().isEmpty());
    }

    @Test
    void fileDeletedOutsideBlueJIsReportedDeleted() {
        openDefault(disk("A", "h1", "B", "h2"));
        sink.clear();

        tracker.reconcile(PROJECT, disk("A", "h1"));

        assertEquals(List.of(CLASS_DELETED, PROJECT_MODIFIED), sink.types());
        assertEquals("B", sink.events().get(0).getClassName());
        assertEquals(Set.of("A"), tracker.knownClasses(PROJECT));
    }

    @Test
    void classDeletedThenRecreatedIsReportedAsCreatedAgain() {
        openDefault(disk("A", "h1"));
        tracker.reconcile(PROJECT, Map.of());
        sink.clear();

        tracker.reconcile(PROJECT, disk("A", "h9"));

        assertEquals(List.of(CLASS_CREATED, PROJECT_MODIFIED), sink.types());
    }

    @Test
    void eventsCarryPackageAndProjectOfTheClass() {
        openDefault(Map.of());
        tracker.packageOpened(SUB, PROJECT, "Shapes", "geo", List.of(), Map.of(), false);
        sink.clear();

        tracker.classSeen(SUB, "geo.Circle");

        LifecycleEvent e = sink.events().get(0);
        assertEquals("Shapes", e.getProjectName());
        assertEquals("geo", e.getPackageName());
        assertEquals("Circle", e.getClassName());
        assertNull(e.getPreviousClassName());
    }

    //robustness

    @Test
    void faultySinkDoesNotBreakTracking() {
        ProjectTracker t = new ProjectTracker(event -> {
            throw new IllegalStateException("queue full");
        }, clock);

        assertDoesNotThrow(() -> {
            t.packageOpened(PROJECT, PROJECT, "Shapes", "", List.of(), Map.of(), false);
            t.classSeen(PROJECT, "A");
        });
        assertEquals(Set.of("A"), t.knownClasses(PROJECT));
    }

    @Test
    void constructorRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new ProjectTracker(null));
        assertThrows(NullPointerException.class, () -> new ProjectTracker(sink, null));
    }

    @Test
    void defaultConstructorUsesSystemClock() {
        RecordingSink s = new RecordingSink();
        long before = System.currentTimeMillis();
        new ProjectTracker(s).packageOpened(PROJECT, PROJECT, "Shapes", "", List.of(), Map.of(), false);
        long at = s.last().getEpochMillis();
        assertTrue(at >= before && at <= System.currentTimeMillis());
    }
}
