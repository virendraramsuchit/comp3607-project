package com.comp3607_project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ActivityTrackerTest {

    private static final long IDLE_MS = 10_000;

    @Test void emptyTrackerHasZeroActiveAndIdle() {
        ActivityTracker t = new ActivityTracker();
        assertEquals(0, t.getActiveTime());
        assertEquals(0, t.getIdleTime());
    }

    @Test void singleEditHasZeroTime() {
        ActivityTracker t = new ActivityTracker();
        t.recordEdit(1000);
        assertEquals(0, t.getActiveTime());
        assertEquals(0, t.getIdleTime());
    }

    @Test void smallGapCountsAsActive() {
        ActivityTracker t = new ActivityTracker();
        t.recordEdit(1_000);
        t.recordEdit(3_000);           // 2s gap < 10s threshold
        assertEquals(2_000, t.getActiveTime());
        assertEquals(0, t.getIdleTime());
    }

    @Test void gapAtExactThresholdCountsAsActive() {
        ActivityTracker t = new ActivityTracker();
        t.recordEdit(0);
        t.recordEdit(IDLE_MS);         // exactly 10s
        assertEquals(IDLE_MS, t.getActiveTime());
        assertEquals(0, t.getIdleTime());
    }

    @Test void largeGapSplitsIntoActiveAndIdle() {
        ActivityTracker t = new ActivityTracker();
        t.recordEdit(0);
        t.recordEdit(IDLE_MS + 5_000); // 15s gap
        assertEquals(IDLE_MS, t.getActiveTime());
        assertEquals(5_000, t.getIdleTime());
    }

    @Test void multipleEditsSumActiveAndIdle() {
        ActivityTracker t = new ActivityTracker();
        t.recordEdit(0);
        t.recordEdit(2_000);           // 2s active
        t.recordEdit(4_000);           // 2s active
        t.recordEdit(20_000);          // 16s = 10s active + 6s idle
        assertEquals(2_000 + 2_000 + IDLE_MS, t.getActiveTime());
        assertEquals(16_000 - IDLE_MS, t.getIdleTime());
    }
}
