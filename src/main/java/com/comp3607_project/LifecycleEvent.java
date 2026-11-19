package com.comp3607_project;

import java.time.Instant;
import java.util.Objects;

/**
 * One immutable, time-stamped project or class lifecycle event.
 */
// One immutable, time-stamped lifecycle event.
public final class LifecycleEvent {

        /**
     * When the event happened.
     */
    private final long epochMillis;

        /**
     * The same time as an ISO-8601 UTC string.
     */
    private final String timestamp;

        /**
     * The kind of event. Never {@code null}.
     */
    private final LifecycleEventType type;

        /**
     * The project's name.
     */
    private final String projectName;

        /**
     * The project's folder.
     */
    private final String projectPath;

        /**
     * The package name ({@code ""} for the default package).
     */
    private final String packageName;

        /**
     * The class name, for class events.
     */
    private final String className;

        /**
     * The old name, for {@code CLASS_RENAMED} only.
     */
    private final String previousClassName;

        /**
     * Extra information, e.g. {@code existingClasses=3} or {@code openDurationMillis=…}.
     */
    private final String detail;

        /**
     * Creates an event.
     *
     * @param epochMillis a time in ms since 1 January 1970 UTC
     * @param type the lifecycle event type
     * @param projectName the project name
     * @param projectPath the project folder
     * @param packageName the package name ({@code ""} for the default package)
     * @param className the class name
     * @param previousClassName the old class name (renames only)
     * @param detail extra information, may be null
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public LifecycleEvent(long epochMillis, LifecycleEventType type, String projectName,
                          String projectPath, String packageName, String className,
                          String previousClassName, String detail) {
        this.epochMillis = epochMillis;
        this.timestamp = Instant.ofEpochMilli(epochMillis).toString();
        this.type = Objects.requireNonNull(type, "type");
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.packageName = packageName;
        this.className = className;
        this.previousClassName = previousClassName;
        this.detail = detail;
    }

        /**
     * Returns the time in ms.
     * @return the time in ms
     */
    public long getEpochMillis() { return epochMillis; }

        /**
     * Returns the ISO-8601 time.
     * @return the ISO-8601 time
     */
    public String getTimestamp() { return timestamp; }

        /**
     * Returns the event type.
     * @return the event type
     */
    public LifecycleEventType getType() { return type; }

        /**
     * Returns the project name.
     * @return the project name
     */
    public String getProjectName() { return projectName; }

        /**
     * Returns the project folder.
     * @return the project folder
     */
    public String getProjectPath() { return projectPath; }

        /**
     * Returns the package name.
     * @return the package name
     */
    public String getPackageName() { return packageName; }

        /**
     * Returns the class name.
     * @return the class name
     */
    public String getClassName() { return className; }

        /**
     * Returns the old class name.
     * @return the old class name
     */
    public String getPreviousClassName() { return previousClassName; }

        /**
     * Returns the extra detail.
     * @return the extra detail
     */
    public String getDetail() { return detail; }

        /**
     * Returns the time, type and every field that is present, e.g. {@code … CLASS_RENAMED
     * project=Shapes class=Circle previous=Round}.
     * @return a one-line description of the event
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp).append(' ').append(type.name());
        if (projectName != null) sb.append(" project=").append(projectName);
        if (packageName != null && !packageName.isEmpty()) sb.append(" package=").append(packageName);
        if (className != null) sb.append(" class=").append(className);
        if (previousClassName != null) sb.append(" previous=").append(previousClassName);
        if (detail != null) sb.append(" (").append(detail).append(')');
        return sb.toString();
    }
}
