package com.comp3607_project;

import java.util.List;
import java.util.ArrayList;

public class ActivityTracker
{
    
    private static final long IDLE_THRESHOLD_MS = 10000;

    private final List<Long> timestamps;

    public ActivityTracker()
    {
        timestamps = new ArrayList<>();
    }

    public void recordEdit(long timestampMs)
    {
        timestamps.add(timestampMs);
    }

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



