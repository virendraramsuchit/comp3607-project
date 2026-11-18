package com.comp3607_project;

import java.util.HashSet;
import java.util.Set;

import bluej.extensions2.BClass;
import bluej.extensions2.ProjectNotOpenException;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;
/**
 * Makes sure every class editor has a {@link TypingListener} attached, so keystrokes reach the
 * {@link ActivityTracker}. Each class is attached only once.
 */
public class ClassTracker implements ClassListener {

/**
 * Passed to each new TypingListener.
 */
    private final ActivityTracker tracker;
/**
 * Names of classes that already have a listener.
 */
    private final Set<String> attached = new HashSet<>();
/**
 * Creates a class tracker that feeds {@code tracker}.
 *
 * @param tracker the tracker that receives the data
 */
    public ClassTracker(ActivityTracker tracker) {
        this.tracker = tracker;
    }
/**
 * Adds a TypingListener to the class's editor if it has none yet. If the editor or project is
 * not ready, does nothing so a later event can retry.
 *
 * @param c the BlueJ class whose editor gets a listener
 */
    public void attach(BClass c) {
        try {
            String name = c.getName();
            if (attached.contains(name)) {
                return;
            }

            JavaEditor editor = c.getJavaEditor();
            if (editor == null) {
                return; 
            }

            editor.addDocumentListener(new TypingListener(tracker));
            attached.add(name);
            System.out.println("Attached to: " + name);
        } catch (ProjectNotOpenException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
/**
 * Forgets the removed class.
 *
 * @param event BlueJ's class notification
 */
    @Override
    public void classRemoved(ClassEvent event) {
        attached.remove(event.getBClass().getName());
    }
/**
 * Updates the stored name after a rename.
 *
 * @param event BlueJ's class notification
 */
    @Override
    public void classNameChanged(ClassEvent event) {
        if (attached.remove(event.getOldName())) {
            attached.add(event.getBClass().getName());
        }

    }
/**
 * Calls {@code attach} for the class.
 *
 * @param event BlueJ's class notification
 */
    @Override
    public void classStateChanged(ClassEvent event) {
        attach(event.getBClass());
    }

}

