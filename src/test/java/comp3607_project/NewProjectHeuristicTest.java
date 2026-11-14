package comp3607_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.comp3607_project.NewProjectHeuristic;

class NewProjectHeuristicTest {

    private static final long NOW = 1_800_000_000_000L;

    @TempDir
    Path project;

    private void writePackageFile(long modifiedMillis) throws IOException {
        Path file = project.resolve("package.bluej");
        Files.writeString(file, "#BlueJ package file");
        Files.setLastModifiedTime(file, FileTime.fromMillis(modifiedMillis));
    }

    @Test
    void emptyDefaultPackageJustWrittenIsNew() throws IOException {
        writePackageFile(NOW - 2_000);
        assertTrue(NewProjectHeuristic.looksNew(project, "", 0, NOW));
    }

    @Test
    void slightlyFutureTimestampStillCountsAsNew() throws IOException {
        writePackageFile(NOW + 500);
        assertTrue(NewProjectHeuristic.looksNew(project, "", 0, NOW));
    }

    @Test
    void oldPackageFileIsNotNew() throws IOException {
        writePackageFile(NOW - NewProjectHeuristic.NEW_PROJECT_WINDOW_MILLIS - 1);
        assertFalse(NewProjectHeuristic.looksNew(project, "", 0, NOW));
    }

    @Test
    void projectWithClassesIsNotNew() throws IOException {
        writePackageFile(NOW);
        assertFalse(NewProjectHeuristic.looksNew(project, "", 1, NOW));
    }

    @Test
    void namedSubPackageIsNotNew() throws IOException {
        writePackageFile(NOW);
        assertFalse(NewProjectHeuristic.looksNew(project, "shapes", 0, NOW));
        assertFalse(NewProjectHeuristic.looksNew(project, null, 0, NOW));
    }

    @Test
    void missingPackageFileIsNotNew() {
        assertFalse(NewProjectHeuristic.looksNew(project, "", 0, NOW));
    }
}