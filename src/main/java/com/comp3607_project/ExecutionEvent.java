package com.comp3607_project;

public class ExecutionEvent implements TelemetryEvent {

    private String className;
    private String methodName;
    private String outcome;
    private long timestamp;

    public ExecutionEvent(String className, String methodName, String outcome){

        this.className = className;
        this.methodName = methodName;
        this.outcome = outcome;
        this.timestamp = System.currentTimeMillis();
    }

    public String getClassName(){return className;}
    public String getMethodName(){return methodName;}
    public String getOutcome(){return outcome;}

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
    String method = (methodName == null) ? "(constructor)" : methodName;
        return "RUN  " + timeFormat(timestamp) +
                " class=" + className + " method=" + method + " outcome=" + outcome;
    }

}
