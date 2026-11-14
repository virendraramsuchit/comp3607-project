package comp3607_project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.comp3607_project.LifecycleEvent;
import com.comp3607_project.LifecycleEventType;

class LifecycleEventTest {

    @Test
    void storesAllFieldsAndIsoTimestamp() {
        LifecycleEvent e = new LifecycleEvent(0L, LifecycleEventType.CLASS_RENAMED, "Shapes",
                "/p/Shapes", "geo", "Circle", "Round", "note");

        assertEquals(0L, e.getEpochMillis());
        assertEquals("1970-01-01T00:00:00Z", e.getTimestamp());
        assertEquals(LifecycleEventType.CLASS_RENAMED, e.getType());
        assertEquals("Shapes", e.getProjectName());
        assertEquals("/p/Shapes", e.getProjectPath());
        assertEquals("geo", e.getPackageName());
        assertEquals("Circle", e.getClassName());
        assertEquals("Round", e.getPreviousClassName());
        assertEquals("note", e.getDetail());
    }

    @Test
    void toStringIncludesEveryPresentField() {
        LifecycleEvent e = new LifecycleEvent(0L, LifecycleEventType.CLASS_RENAMED, "Shapes",
                "/p/Shapes", "geo", "Circle", "Round", "note");

        assertEquals("1970-01-01T00:00:00Z CLASS_RENAMED project=Shapes package=geo class=Circle"
                + " previous=Round (note)", e.toString());
    }

    @Test
    void toStringLeavesOutMissingFieldsAndDefaultPackage() {
        LifecycleEvent e = new LifecycleEvent(1000L, LifecycleEventType.PROJECT_OPENED, null,
                null, "", null, null, null);

        assertEquals("1970-01-01T00:00:01Z PROJECT_OPENED", e.toString());
        assertNull(e.getProjectName());
    }

    @Test
    void typeIsRequired() {
        assertThrows(NullPointerException.class,
                () -> new LifecycleEvent(0L, null, "p", "/p", "", null, null, null));
    }

    @Test
    void enumCoversCreateModifyDeleteForProjectsAndClasses() {
        for (String name : new String[] {"PROJECT_CREATED", "PROJECT_MODIFIED", "PROJECT_DELETED",
                "CLASS_CREATED", "CLASS_MODIFIED", "CLASS_DELETED"}) {
            assertEquals(name, LifecycleEventType.valueOf(name).name());
        }
    }
}
