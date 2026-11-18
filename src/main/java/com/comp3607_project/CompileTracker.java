package com.comp3607_project;

import java.util.HashMap;
import java.util.Map;

import bluej.extensions2.event.CompileListener;
/**
 * Receives BlueJ's compile notifications and records a {@link CompileEvent} for each one. For
 * errors, records the file, line number and message, and works out an error category from the
 * message, because BlueJ does not expose javac error codes.
 */
public class CompileTracker implements CompileListener {
/**
 * Number of successful compiles this session.
 */
    private int successCount = 0;
/**
 * Number of failed compiles this session.
 */
    private int failCount = 0;
/**
 * Number of errors in each category.
 */
    private Map<String, Integer> errorCounts = new HashMap<>();

/**
 * Where compile events are sent.
 */
    private final EventSink sink;

/**
 * Creates a tracker that sends events to {@code sink}.
 *
 * @param sink where events are sent
 */
    public CompileTracker(EventSink sink) {
        this.sink = sink;
    }

/**
 * Creates a tracker with a sink that discards events (used by unit tests).
 */
    public CompileTracker() {
        this(event -> { /* no-op */ });
    }

/**
 * Pairs of (text to look for in a javac message, category name), e.g. {@code "is already
 * defined"} -> {@code "name already defined"}.
 */
    private static final String[][] PATCHES = {
            { "is already defined", "name already defined" },
            { "cannot be applied to given types", "method cannot be applied to given types" },
            { "might not have been initialized", "variable might not have been initialized" },
            { "cannot be referenced from a static context", "non-static reference from static context" },
            { "should be declared in a file named", "class name does not match file name" },
            { "unreported exception", "unreported exception" },
            { "has private access in", "private access violation" },
            { "cannot assign a value to final variable", "assignment to final variable" },
            { "cannot be dereferenced", "primitive cannot be dereferenced" },
            { "does not exist", "package does not exist" },
    };
/**
 * Returns the error category for a javac message. Returns {@code "unknown error"} for {@code
 * null}. Uses only the first line, returns the matching {@code PATCHES} category if one
 * matches, otherwise the text before the first colon, trimmed.
 *
 * @param message the full javac error message, may be null
 * @return the error category
 */
    String errorKey(String message) {
        if (message == null) return "unknown error";
        int newline = message.indexOf('\n');
        if (newline != -1) message = message.substring(0, newline);
        for (String[] patch : PATCHES) {
            if (message.contains(patch[0])) return patch[1];
        }
        int colon = message.indexOf(':');
        if (colon != -1) message = message.substring(0, colon);
        return message.trim();
    }

/**
 * Records a {@code STARTED} event.
 *
 * @param event BlueJ's compile notification
 */
    @Override
    @Generated
    public void compileStarted(bluej.extensions2.event.CompileEvent event) {
        System.out.println("Compile Started");
        sink.record(new com.comp3607_project.CompileEvent(
                com.comp3607_project.CompileEvent.Outcome.STARTED,
                null, 0, null, null));
    }
 
/**
 * Increments {@code successCount} and records a {@code SUCCESS} event.
 *
 * @param event BlueJ's compile notification
 */
    @Override
    @Generated
    public void compileSucceeded(bluej.extensions2.event.CompileEvent event) {
        successCount++;
        System.out.println("Compile Succeeded");
        System.out.println("Successes: " + successCount + " | Failures: " + failCount
                + " | Errors: " + errorCounts);
        sink.record(new com.comp3607_project.CompileEvent(
                com.comp3607_project.CompileEvent.Outcome.SUCCESS,
                null, 0, null, null));
    }
/**
 * Increments {@code failCount} and records a {@code FAILURE} event.
 *
 * @param event BlueJ's compile notification
 */
    @Override
    @Generated
    public void compileFailed(bluej.extensions2.event.CompileEvent event) {
        failCount++;
        System.out.println("Compile Failed");
        System.out.println("Successes: " + successCount + " | Failures: " + failCount
                + " | Errors: " + errorCounts);
        sink.record(new com.comp3607_project.CompileEvent(
                com.comp3607_project.CompileEvent.Outcome.FAILURE,
                null, 0, null, null));
    }
/**
 * Records an {@code ERROR} event with the first file's name, the line number, the full message
 * and its category, and counts the category in {@code errorCounts}.
 *
 * @param event BlueJ's compile notification, with the file, line and message
 */
    @Override
    @Generated
    public void compileError(bluej.extensions2.event.CompileEvent event) {
        String fileName = (event.getFiles() != null && event.getFiles().length > 0)
                ? event.getFiles()[0].getName() : null;
        int line = event.getErrorLineNumber();
        String rawMessage = event.getErrorMessage();
        String key = errorKey(rawMessage);

        System.out.println("Compile Error | file: " + fileName
                + " | line: " + line
                + " | message: " + rawMessage
                + " | categoryTest: " + key);

        errorCounts.merge(key, 1, Integer::sum);

        sink.record(new com.comp3607_project.CompileEvent(
                com.comp3607_project.CompileEvent.Outcome.ERROR,
                fileName, line, rawMessage, key));
    }
/**
 * Records a {@code WARNING} event.
 *
 * @param event BlueJ's compile notification
 */
    @Override
    @Generated
    public void compileWarning(bluej.extensions2.event.CompileEvent event) {
        System.out.println("Compile Warning");
        sink.record(new com.comp3607_project.CompileEvent(
                com.comp3607_project.CompileEvent.Outcome.WARNING,
                null, 0, null, null));
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.CONSTRUCTOR})
    public @interface Generated {}
}