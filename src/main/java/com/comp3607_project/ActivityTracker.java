package com.comp3607_project;

import java.util.ArrayList;
import java.util.List;
/**
 * Stores the time of each keystroke-level edit and splits the time between them into active
 * and idle time. A gap of up to 10 seconds counts as active; anything beyond 10 seconds counts
 * as idle.
 */
public class ActivityTracker
{
/**
 * Idle threshold, {@code 10000} ms (10 seconds), as set in the project scope.
 */
    private static final long IDLE_THRESHOLD_MS = 10000;
/**
 * Time of each recorded edit, in order.
 */
    private final List<Long> timestamps;
/**
 * Creates a tracker with no edits.
 */
    public ActivityTracker()
    {
        timestamps = new ArrayList<>();
    }
/**
 * Adds the time of one edit.
 *
 * @param timestampMs the time of the edit in ms since the epoch
 */
    public void recordEdit(long timestampMs)
    {
        timestamps.add(timestampMs);
    }
/**
 * Sum over consecutive edits of {@code min(gap, 10000)}, in ms.
 * @return total active time in ms
 */
    public long getActiveTime()
    {
        long activeTime = 0;

        for (int i = 1; i < timestamps.size(); i++)
        {
            long gap = timestamps.get(i) - timestamps.get(i - 1);

            activeTime = activeTime + Math.min(gap, IDLE_THRESHOLD_MS);
        }

        return activeTime;
    }
/**
 * Sum over consecutive edits of {@code max(0, gap − 10000)}, in ms.
 * @return total idle time in ms
 */
    public long getIdleTime()
    {
        long idleTime = 0;

        for (int i = 1; i < timestamps.size(); i++)
        {
            long gap = timestamps.get(i) - timestamps.get(i - 1);

            idleTime = idleTime + Math.max(0, gap - IDLE_THRESHOLD_MS);
        }

        return idleTime;
    }
}



