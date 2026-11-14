package comp3607_project;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import comp3607_project.LifecycleEvent;
import comp3607_project.LifecycleEventType;
import comp3607_project.LifecycleSink;

final class RecordingSink implements LifecycleSink {

    private final List<LifecycleEvent> events = new ArrayList<>();

    @Override
    public synchronized void accept(LifecycleEvent event) {
        events.add(event);
    }

    synchronized List<LifecycleEvent> events() {
        return new ArrayList<>(events);
    }

    synchronized List<LifecycleEventType> types() {
        return events.stream().map(LifecycleEvent::getType).collect(Collectors.toList());
    }

    synchronized void clear() {
        events.clear();
    }

    synchronized LifecycleEvent last() {
        return events.get(events.size() - 1);
    }
}
