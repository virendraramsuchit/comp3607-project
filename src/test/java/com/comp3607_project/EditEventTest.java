package com.comp3607_project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class EditEventTest {

    @Test
    void constructorStoresAllFields() {
        EditEvent e = new EditEvent("Foo", 5, 1000L);
        assertEquals("Foo", e.getClassName());
        assertEquals(5, e.getNumCharsChanged());
        assertEquals(1000L, e.getIdleMillis());
    }

    @Test
    void timestampIsSetCloseToNow() {
        long before = System.currentTimeMillis();
        EditEvent e = new EditEvent("Bar", 1, 0L);
        long after = System.currentTimeMillis();

        assertTrue(e.getTimeStamp() >= before);
        assertTrue(e.getTimeStamp() <= after);
    }

    @Test
    void idleMillisIsPreserved_notAlwaysZero() {
       
        EditEvent e = new EditEvent("X", 3, 2500L);
        assertEquals(2500L, e.getIdleMillis());
    }

    @Test
    void toStringContainsAllFields() {
        EditEvent e = new EditEvent("Foo", 7, 500L);
        String s = e.toString();
        assertTrue(s.startsWith("EDIT "));
        assertTrue(s.contains("class=Foo"));
        assertTrue(s.contains("charsChanged=7"));
        assertTrue(s.contains("idleMs=500"));
    }

    @Test
    void implementsTelemetryEventContract() {
        TelemetryEvent e = new EditEvent("Foo", 1, 0L);
        assertTrue(e.getTimeStamp() > 0);
    }
}