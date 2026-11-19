package com.comp3607_project;

/**
 * One method or constructor run in BlueJ, as recorded by {@link ExecutionListener}.
 */
public class ExecutionEvent implements TelemetryEvent {

        /**
     * The class whose method or constructor ran.
     */
    private String className;

        /**
     * The method name, or {@code null} for a constructor.
     */
    private String methodName;

        /**
     * {@code "RAN NORMALLY"}, {@code "EXCEPTION"} or {@code "OTHER"}.
     */
    private String outcome;

        /**
     * When the event was created.
     */
    private long timestamp;

        /**
     * Creates an execution event time-stamped with the current time.
     *
     * @param className the class name
     * @param methodName the method name, or null for a constructor
     * @param outcome {@code "RAN NORMALLY"}, {@code "EXCEPTION"} or {@code "OTHER"}
     */
    public ExecutionEvent(String className, String methodName, String outcome){

        this.className = className;
        this.methodName = methodName;
        this.outcome = outcome;
        this.timestamp = System.currentTimeMillis();
    }

        /**
     * Returns the class name.
     * @return the class name
     */
    public String getClassName(){return className;}

        /**
     * Returns the method name, or {@code null} for a constructor.
     * @return the method name, or {@code null} for a constructor
     */
    public String getMethodName(){return methodName;}

        /**
     * Returns the outcome.
     * @return the outcome
     */
    public String getOutcome(){return outcome;}

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
     * Returns {@code RUN <time> class=<name> method=<name or (constructor)> outcome=<outcome>}.
     * @return a one-line description of the event
     */
    @Override 
    public String toString() {
    String method = (methodName == null) ? "(constructor)" : methodName;
        return "RUN  " + timeFormat(timestamp) +
                " class=" + className + " method=" + method + " outcome=" + outcome;
    }

}
