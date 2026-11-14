package comp3607_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.comp3607_project.SourceFiles;

class SourceFilesTest {

    @TempDir
    Path dir;

    @Test
    void simpleNameDropsPackagePrefix() {
        assertEquals("Foo", SourceFiles.simpleName("a.b.Foo"));
        assertEquals("Foo", SourceFiles.simpleName("Foo"));
    }

    @Test
    void baseNameAcceptsJavaAndStrideSources() {
        assertEquals("Foo", SourceFiles.baseName("Foo.java"));
        assertEquals("Bar", SourceFiles.baseName("Bar.stride"));
    }

    @Test
    void baseNameRejectsOtherFilesAndInvalidNames() {
        assertNull(SourceFiles.baseName("Foo.class"));
        assertNull(SourceFiles.baseName("README.TXT"));
        assertNull(SourceFiles.baseName("package.bluej"));
        assertNull(SourceFiles.baseName("1Foo.java"));
        assertNull(SourceFiles.baseName("My Class.java"));
        assertNull(SourceFiles.baseName(".java"));
    }

    @Test
    void isValidClassName() {
        assertTrue(SourceFiles.isValidClassName("Foo"));
        assertTrue(SourceFiles.isValidClassName("_foo$1"));
        assertFalse(SourceFiles.isValidClassName(null));
        assertFalse(SourceFiles.isValidClassName(""));
        assertFalse(SourceFiles.isValidClassName("9Lives"));
        assertFalse(SourceFiles.isValidClassName("a-b"));
    }

    @Test
    void snapshotListsOnlySourceFilesDirectlyInFolder() throws IOException {
        Files.writeString(dir.resolve("A.java"), "class A {}");
        Files.writeString(dir.resolve("B.stride"), "<stride/>");
        Files.writeString(dir.resolve("A.class"), "binary");
        Files.writeString(dir.resolve("README.TXT"), "readme");
        Files.createDirectory(dir.resolve("Folder.java")); // a folder, not a file
        Files.createDirectories(dir.resolve("sub"));
        Files.writeString(dir.resolve("sub").resolve("C.java"), "class C {}");

        Map<String, String> snap = SourceFiles.snapshot(dir);

        assertEquals(Set.of("A", "B"), snap.keySet());
    }

    @Test
    void fingerprintChangesOnlyWhenContentChanges() throws IOException {
        Path a = dir.resolve("A.java");
        Files.writeString(a, "class A {}");
        String first = SourceFiles.fingerprint(a);

        Files.writeString(a, "class A {}");
        assertEquals(first, SourceFiles.fingerprint(a));

        Files.writeString(a, "class A { int x; }");
        assertNotEquals(first, SourceFiles.fingerprint(a));
        assertEquals(64, first.length()); // SHA-256 in hex
    }

    @Test
    void snapshotOfMissingFolderThrows() {
        assertThrows(IOException.class, () -> SourceFiles.snapshot(dir.resolve("missing")));
    }

    @Test
    void fingerprintOfMissingFileThrows() {
        assertThrows(IOException.class, () -> SourceFiles.fingerprint(dir.resolve("Nope.java")));
    }
}
