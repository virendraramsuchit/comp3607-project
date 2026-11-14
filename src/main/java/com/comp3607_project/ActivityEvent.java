package com.comp3607_project;

public class ActivityEvent implements TelemetryEvent {

    private final String packageName;
    private final long activeMillis;
    private final long idleMillis;
    private final long timestamp;

    public ActivityEvent(String packageName, long activeMillis, long idleMillis) {
        this.packageName = packageName;
        this.activeMillis = activeMillis;
        this.idleMillis = idleMillis;
        this.timestamp = System.currentTimeMillis();
    }

    public String getPackageName() { return packageName; }
    public long getActiveMillis() { return activeMillis; }
    public long getIdleMillis() { return idleMillis; }

    @Override public long getTimeStamp() { return timestamp; }

    @Override public String toString() {
        return "ACTIVITY package=" + packageName
             + " activeMs=" + activeMillis
             + " idleMs=" + idleMillis;
    }
}
