package com.comp3607_project;
/**
 * One edit to a class, as detected by {@link EditPoller}.
 */

public class EditEvent implements TelemetryEvent {
/**
 * The edited class.
 */
    private String className;
/**
 * Characters inserted, deleted or replaced since the previous check.
 */
    private int numCharsChanged;
/**
 * Time since the previous recorded edit to the same class (0 for the first).
 */
    private long idleMillis; 
/**
 * When the event was created.
 */
    private long timestamp;
/**
 * Creates an edit event time-stamped with the current time.
 *
 * @param className the class name
 * @param numCharsChanged the number of characters changed
 * @param idleMillis time since the previous edit to this class, in ms
 */
    public EditEvent(String className, int numCharsChanged, long idleMillis){

        this.className = className;
        this.numCharsChanged = numCharsChanged;
        this.idleMillis = idleMillis;
        this.timestamp = System.currentTimeMillis();
    }
/**
 * Returns the class name.
 * @return the class name
 */
    public String getClassName(){return className;}
/**
 * Returns the number of characters changed.
 * @return the number of characters changed
 */
    public int getNumCharsChanged(){return numCharsChanged;}
/**
 * Returns the time since the previous edit, in ms.
 * @return the time since the previous edit, in ms
 */
    public long  getIdleMillis() { return idleMillis; }
/**
 * Formats a time as a local date-time string.
 *
 * @param epochMillis a time in ms since 1 January 1970 UTC
 * @return the time as a local date-time string
 */
    private static String timeFormat(long epochMillis) {
    return java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(epochMillis),
                    java.time.ZoneId.systemDefault()
                    ).toString();
    }
    /**
 * Returns the creation time.
 * @return the creation time
 */
    @Override 
    public long getTimeStamp(){return timestamp;}
/**
 * Returns {@code EDIT <time> class=<name> charsChanged=<n> idleMs=<n>}.
 * @return a one-line description of the event
 */
    @Override 
    public String toString() {
    return "EDIT " + timeFormat(timestamp)
         + " class=" + className
         + " charsChanged=" + numCharsChanged
         + " idleMs=" + idleMillis;
    }

}
