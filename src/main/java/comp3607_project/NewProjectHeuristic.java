package comp3607_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public final class NewProjectHeuristic {

    public static final long NEW_PROJECT_WINDOW_MILLIS = 10_000;

    private NewProjectHeuristic() { }

    public static boolean looksNew(Path projectDir, String packageName, int classCount, long nowMillis) {
        if (packageName == null || !packageName.isEmpty() || classCount != 0) {
            return false;
        }
        try {
            long modified = Files.getLastModifiedTime(projectDir.resolve("package.bluej")).toMillis();
            // abs(): file-system timestamps can be a little ahead of the JVM clock
            return Math.abs(nowMillis - modified) <= NEW_PROJECT_WINDOW_MILLIS;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
