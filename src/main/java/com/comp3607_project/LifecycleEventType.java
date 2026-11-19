package com.comp3607_project;

/**
 *
 */
public enum LifecycleEventType {

        /**
     * A new, empty project was created (inferred by {@link NewProjectHeuristic}).
     */
    PROJECT_CREATED,

        /**
     * A project was opened.
     */
    PROJECT_OPENED,

        /**
     * A class in the project was created, changed, renamed or deleted.
     */
    PROJECT_MODIFIED,

        /**
     * A project was closed, or BlueJ shut down while it was open.
     */
    PROJECT_CLOSED,

        /**
     * The project's folder no longer exists.
     */
    PROJECT_DELETED,

        /**
     * A class was created in BlueJ or a source file appeared on disk.
     */
    CLASS_CREATED,

        /**
     * A class's source content changed.
     */
    CLASS_MODIFIED,

        /**
     * A class was renamed.
     */
    CLASS_RENAMED,

        /**
     * A class was removed in BlueJ or its file was deleted on disk.
     */
    CLASS_DELETED
}
