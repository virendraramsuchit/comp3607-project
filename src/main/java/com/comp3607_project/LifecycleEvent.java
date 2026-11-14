package com.comp3607_project;

import java.time.Instant;
import java.util.Objects;

// One immutable, time-stamped lifecycle event.
public final class LifecycleEvent {

    private final long epochMillis;
    private final String timestamp;
    private final LifecycleEventType type;
    private final String projectName;
    private final String projectPath;
    private final String packageName;
    private final String className;
    private final String previousClassName;
    private final String detail;

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

    public long getEpochMillis() { return epochMillis; }
    public String getTimestamp() { return timestamp; }
    public LifecycleEventType getType() { return type; }
    public String getProjectName() { return projectName; }
    public String getProjectPath() { return projectPath; }
    public String getPackageName() { return packageName; }
    public String getClassName() { return className; }
    public String getPreviousClassName() { return previousClassName; }
    public String getDetail() { return detail; }

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
