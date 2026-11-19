package com.comp3607_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Decides whether a project BlueJ has just opened was also just created, because BlueJ has no
 * "project created" event. It cannot be instantiated.
 */
public final class NewProjectHeuristic {

        /**
     * {@code 10000} ms: how recently {@code package.bluej} must have been written.
     */
    public static final long NEW_PROJECT_WINDOW_MILLIS = 10_000;

    private NewProjectHeuristic() { }

        /**
     * Returns {@code true} only for the default package with no classes, whose {@code
     * package.bluej} file was written within 10 seconds of {@code nowMillis}. Returns {@code
     * false} if the file cannot be read.
     *
     * @param projectDir the project folder
     * @param packageName the package name ({@code ""} for the default package)
     * @param classCount how many classes BlueJ reports in the package
     * @param nowMillis the current time in ms
     * @return {@code true} if the project appears to have just been created
     */
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
