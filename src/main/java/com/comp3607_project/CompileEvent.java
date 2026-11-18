package com.comp3607_project;
/**
 * One compile notification from BlueJ, as recorded by {@link CompileTracker}.
 */
public class CompileEvent implements TelemetryEvent {

    public enum Outcome {
        STARTED,
        SUCCESS,
        FAILURE,
        ERROR,
        WARNING
    }
/**
 * The kind of notification.
 */
    private final Outcome outcome;
/**
 * The source file with the error; {@code null} except for {@code ERROR}.
 */
    private final String fileName;      
/**
 * The error's line number; {@code 0} when not applicable.
 */
    private final int lineNumber;      
/**
 * The full javac message; may be {@code null}.
 */
    private final String message;       
/**
 * The error category from {@code CompileTracker.errorKey()}; {@code null} except for {@code
 * ERROR}.
 */
    private final String errorKey;      
/**
 * When the event was created.
 */

    private final long timestamp;
/**
 * Creates a compile event time-stamped with the current time.
 *
 * @param outcome the kind of compile notification
 * @param fileName the source file name, or null
 * @param lineNumber the line number, or 0 if not applicable
 * @param message the full javac message, may be null
 * @param errorKey the error category, or null
 */
    public CompileEvent(Outcome outcome, String fileName, int lineNumber,
                        String message, String errorKey) {
        this.outcome = outcome;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.message = message;
        this.errorKey = errorKey;
        this.timestamp = System.currentTimeMillis();
    }
/**
 * Returns the outcome.
 * @return the outcome
 */
    public Outcome getOutcome() { return outcome; }
/**
 * Returns the file name.
 * @return the file name
 */
    public String getFileName() { return fileName; }
/**
 * Returns the line number.
 * @return the line number
 */
    public int getLineNumber() { return lineNumber; }
/**
 * Returns the full message.
 * @return the full message
 */
    public String getMessage() { return message; }
/**
 * Returns the error category.
 * @return the error category
 */
    public String getErrorKey() { return errorKey; }
/**
 * Returns the creation time.
 * @return the creation time
 */
    @Override public long getTimeStamp() { return timestamp; }
/**
 * For errors, {@code COMPILE ERROR file=<file> line=<n> key=<category>}; otherwise {@code
 * COMPILE <outcome>}.
 * @return a one-line description of the event
 */
    @Override public String toString() {
        if (outcome == Outcome.ERROR) {
            return "COMPILE ERROR file=" + fileName + " line=" + lineNumber
                 + " key=" + errorKey;
        }
        return "COMPILE " + outcome;
    }
}