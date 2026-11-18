package com.comp3607_project;
/**
 * Active and idle typing time for a package, recorded by {@link PackageTracker} when the
 * package closes.
 */
public class ActivityEvent implements TelemetryEvent {
/**
 * The package that closed.
 */
    private final String packageName;
/**
 * Active time, in ms.
 */
    private final long activeMillis;
/**
 * Idle time, in ms.
 */
    private final long idleMillis;
/**
 * When the event was created.
 */
    private final long timestamp;
/**
 * Creates an activity event time-stamped with the current time.
 *
 * @param packageName the package name ({@code ""} for the default package)
 * @param activeMillis the active time in ms
 * @param idleMillis the idle time in ms
 */
    public ActivityEvent(String packageName, long activeMillis, long idleMillis) {
        this.packageName = packageName;
        this.activeMillis = activeMillis;
        this.idleMillis = idleMillis;
        this.timestamp = System.currentTimeMillis();
    }
/**
 * Returns the package name.
 * @return the package name
 */
    public String getPackageName() { return packageName; }
/**
 * Returns the active time.
 * @return the active time
 */
    public long getActiveMillis() { return activeMillis; }
/**
 * Returns the idle time.
 * @return the idle time
 */
    public long getIdleMillis() { return idleMillis; }
/**
 * Returns the creation time.
 * @return the creation time
 */
    @Override public long getTimeStamp() { return timestamp; }
/**
 * Returns {@code ACTIVITY package=<name> activeMs=<n> idleMs=<n>}.
 * @return a one-line description of the event
 */
    @Override public String toString() {
        return "ACTIVITY package=" + packageName
             + " activeMs=" + activeMillis
             + " idleMs=" + idleMillis;
    }
}
