package com.comp3607_project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ExecutionEventTest {

    @Test
    void constructorStoresAllFields() {
        ExecutionEvent e = new ExecutionEvent("Foo", "bar", "RAN NORMALLY");
        assertEquals("Foo", e.getClassName());
        assertEquals("bar", e.getMethodName());
        assertEquals("RAN NORMALLY", e.getOutcome());
    }

    @Test
    void nullMethodNameRendersAsConstructor() {
        ExecutionEvent e = new ExecutionEvent("Foo", null, "RAN NORMALLY");
        assertTrue(e.toString().contains("method=(constructor)"));
    }

    @Test
    void toStringContainsOutcome() {
        ExecutionEvent e = new ExecutionEvent("Foo", "m", "EXCEPTION");
        String s = e.toString();
        assertTrue(s.startsWith("RUN "));
        assertTrue(s.contains("class=Foo"));
        assertTrue(s.contains("method=m"));
        assertTrue(s.contains("outcome=EXCEPTION"));
    }

    @Test
    void timestampIsSet() {
        long before = System.currentTimeMillis();
        ExecutionEvent e = new ExecutionEvent("Foo", "m", "OTHER");
        assertTrue(e.getTimeStamp() >= before);
    }
}
