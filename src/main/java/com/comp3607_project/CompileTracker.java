package com.comp3607_project;

import java.util.HashMap;
import java.util.Map;

import bluej.extensions2.event.CompileListener;

public class CompileTracker implements CompileListener {

    private int successCount = 0;
    private int failCount = 0;
    private Map<String, Integer> errorCounts = new HashMap<>();

    
    private final EventSink sink;

    
    public CompileTracker(EventSink sink) {
        this.sink = sink;
    }

    
    public CompileTracker() {
        this(event -> { /* no-op */ });
    }

  
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


    @Override
    @Generated
    public void compileStarted(bluej.extensions2.event.CompileEvent event) {
        System.out.println("Compile Started");
        sink.record(new com.comp3607_project.CompileEvent(
                com.comp3607_project.CompileEvent.Outcome.STARTED,
                null, 0, null, null));
    }

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