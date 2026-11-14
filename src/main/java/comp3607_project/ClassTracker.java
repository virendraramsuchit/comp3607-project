package comp3607_project;

import bluej.extensions2.BClass;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;
import bluej.extensions2.ProjectNotOpenException;

import java.util.HashSet;
import java.util.Set;

public class ClassTracker implements ClassListener {

    private final ActivityTracker tracker;
    private final Set<String> attached = new HashSet<>();

    public ClassTracker(ActivityTracker tracker) {
        this.tracker = tracker;
    }

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

    @Override
    public void classRemoved(ClassEvent event) {
        attached.remove(event.getBClass().getName());
    }

    @Override
    public void classNameChanged(ClassEvent event) {
        if (attached.remove(event.getOldName())) {
            attached.add(event.getBClass().getName());
        }

    }

    @Override
    public void classStateChanged(ClassEvent event) {
        attach(event.getBClass());
    }

}

