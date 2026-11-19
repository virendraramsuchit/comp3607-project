package com.comp3607_project;

import bluej.extensions2.editor.DocumentListener;

/**
 * Attached to a class's editor. Records the current time in the {@link ActivityTracker} every
 * time text is replaced.
 */
public class TypingListener implements DocumentListener {

        /**
     * Receives the edit times.
     */
    private final ActivityTracker tracker;

        /**
     * Creates a listener that feeds {@code tracker}.
     *
     * @param tracker the tracker that receives the data
     */
    public TypingListener(ActivityTracker tracker) {
        this.tracker = tracker;
    }

        /**
     * Called by the editor on every text change; records the current time. The arguments are not
     * used.
     *
     * @param a not used (required by DocumentListener)
     * @param oldText the replaced text (not used)
     * @param newText the new text (not used)
     * @param b not used (required by DocumentListener)
     * @param c not used (required by DocumentListener)
     */
    @Override
    public void textReplaced(int a, String oldText, String newText, int b, int c) {

        tracker.recordEdit(System.currentTimeMillis());

    }

}