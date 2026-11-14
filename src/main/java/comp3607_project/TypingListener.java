package comp3607_project;

import bluej.extensions2.editor.DocumentListener;

public class TypingListener implements DocumentListener {

    private final ActivityTracker tracker;

    public TypingListener(ActivityTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public void textReplaced(int a, String oldText, String newText, int b, int c) {

        tracker.recordEdit(System.currentTimeMillis());

    }

}