package com.comp3607_project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ExecutionListenerTest {

    private final ExecutionListener listener = new ExecutionListener(event -> {});

    @Test
    void determineOutcome_normalExit() {
        assertEquals("RAN NORMALLY", listener.determineOutcome("NORMAL_EXIT"));
    }

    @Test
    void determineOutcome_exceptionExit() {
        assertEquals("EXCEPTION", listener.determineOutcome("EXCEPTION_EXIT"));
    }

    @Test
    void determineOutcome_unknownFallsToOther() {
        assertEquals("OTHER", listener.determineOutcome("SOMETHING_ELSE"));
    }

    @Test
    void determineOutcome_emptyStringIsOther() {
        assertEquals("OTHER", listener.determineOutcome(""));
    }
}