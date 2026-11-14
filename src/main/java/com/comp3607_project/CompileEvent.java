package com.comp3607_project;

public class CompileEvent implements TelemetryEvent {

    public enum Outcome {
        STARTED,
        SUCCESS,
        FAILURE,
        ERROR,
        WARNING
    }

    private final Outcome outcome;
    private final String fileName;      
    private final int lineNumber;      
    private final String message;       
    private final String errorKey;      
    private final long timestamp;

    public CompileEvent(Outcome outcome, String fileName, int lineNumber,
                        String message, String errorKey) {
        this.outcome = outcome;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.message = message;
        this.errorKey = errorKey;
        this.timestamp = System.currentTimeMillis();
    }

    public Outcome getOutcome() { return outcome; }
    public String getFileName() { return fileName; }
    public int getLineNumber() { return lineNumber; }
    public String getMessage() { return message; }
    public String getErrorKey() { return errorKey; }

    @Override public long getTimeStamp() { return timestamp; }

    @Override public String toString() {
        if (outcome == Outcome.ERROR) {
            return "COMPILE ERROR file=" + fileName + " line=" + lineNumber
                 + " key=" + errorKey;
        }
        return "COMPILE " + outcome;
    }
}