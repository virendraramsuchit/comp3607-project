package comp3607_project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TypingListenerTest {

    @Test void constructingListenerDoesNotRecord() {
        ActivityTracker tracker = new ActivityTracker();
        new TypingListener(tracker);
        assertEquals(0, tracker.getActiveTime());
    }

    @Test void textReplacedRecordsAnEdit() {
        ActivityTracker tracker = new ActivityTracker();
        TypingListener listener = new TypingListener(tracker);

        listener.textReplaced(0, "", "x", 0, 0);
        listener.textReplaced(0, "x", "xy", 0, 0);

        assertTrue(tracker.getActiveTime() >= 0);
        assertTrue(tracker.getActiveTime() < 10_000);
    }

    @Test void multipleTextReplacedCallsAllRecord() {
        ActivityTracker tracker = new ActivityTracker();
        TypingListener listener = new TypingListener(tracker);
        for (int i = 0; i < 5; i++) {
            listener.textReplaced(0, "", "x", 0, 0);
        }
        assertTrue(tracker.getActiveTime() < 10_000);
    }
}