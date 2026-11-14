package comp3607_project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.comp3607_project.LifecycleEvent;
import com.comp3607_project.LifecycleEventAdapter;
import com.comp3607_project.LifecycleEventType;
import com.comp3607_project.TelemetryEvent;

class LifecycleEventAdapterTest {

    @Test
    void adaptsTimestampFromEpochMillis() {
        LifecycleEvent original = new LifecycleEvent(
            12345L, LifecycleEventType.PROJECT_OPENED,
            "P", "/p", "", null, null, null);

        LifecycleEventAdapter adapter = new LifecycleEventAdapter(original);
        assertEquals(12345L, adapter.getTimeStamp());
    }

    @Test
    void getWrappedReturnsOriginal() {
        LifecycleEvent original = new LifecycleEvent(
            500L, LifecycleEventType.CLASS_CREATED,
            "P", "/p", "pkg", "Foo", null, "detail");

        LifecycleEventAdapter adapter = new LifecycleEventAdapter(original);
        assertSame(original, adapter.getWrapped());
    }

    @Test
    void implementsTelemetryEvent() {
        LifecycleEvent original = new LifecycleEvent(
            100L, LifecycleEventType.PROJECT_OPENED, null, null, "", null, null, null);
        TelemetryEvent event = new LifecycleEventAdapter(original);
        assertEquals(100L, event.getTimeStamp());
    }

    @Test
    void toStringPrefixesWithLifecycle() {
        LifecycleEvent original = new LifecycleEvent(
            1000L, LifecycleEventType.CLASS_CREATED, "P", "/p", "", "Foo", null, null);
        assertTrue(new LifecycleEventAdapter(original).toString().startsWith("LIFECYCLE "));
    }
}
