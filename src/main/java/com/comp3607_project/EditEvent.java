package com.comp3607_project;

public class EditEvent implements TelemetryEvent {

    private String className;
    private int numCharsChanged;
    private long idleMillis; 
    private long timestamp;

    public EditEvent(String className, int numCharsChanged, long idleMillis){

        this.className = className;
        this.numCharsChanged = numCharsChanged;
        this.idleMillis = idleMillis;
        this.timestamp = System.currentTimeMillis();
    }

    public String getClassName(){return className;}
    public int getNumCharsChanged(){return numCharsChanged;}
    public long  getIdleMillis() { return idleMillis; }

    private static String timeFormat(long epochMillis) {
    return java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(epochMillis),
                    java.time.ZoneId.systemDefault()
                    ).toString();
    }
    
    @Override 
    public long getTimeStamp(){return timestamp;}

    @Override 
    public String toString() {
    return "EDIT " + timeFormat(timestamp)
         + " class=" + className
         + " charsChanged=" + numCharsChanged
         + " idleMs=" + idleMillis;
    }

}
