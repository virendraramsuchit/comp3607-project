package com.comp3607_project;

import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;

/**
 * Receives a notification from BlueJ each time a method or constructor run finishes, and
 * records an {@link ExecutionEvent} with its outcome.
 */
public class ExecutionListener implements InvocationFinishedListener {
    
        /**
     * Where execution events are sent.
     */
    private EventSink sink;

        /**
     * Creates a listener that sends events to {@code sink}.
     *
     * @param sink where events are sent
     */
    public ExecutionListener(EventSink sink){

        this.sink = sink;  
    }

        /**
     * Records an {@code ExecutionEvent} with the class name, method name ({@code null} for a
     * constructor) and outcome.
     *
     * @param event BlueJ's notification that a run finished
     */
    @Override 
    public void invocationFinished(InvocationFinishedEvent event){

        String className = event.getClassName();
        String methodName = event.getMethodName();

        String outcome = determineOutcome(
                        event.getEventType().toString()
        );

        ExecutionEvent executionEvent = new ExecutionEvent(className, methodName, outcome);
        
        sink.record(executionEvent);
    }

        /**
     * Maps BlueJ's result type to an outcome: {@code "NORMAL_EXIT"} -> {@code "RAN NORMALLY"},
     * {@code "EXCEPTION_EXIT"} -> {@code "EXCEPTION"}, anything else -> {@code "OTHER"}.
     *
     * @param eventType BlueJ's result type as text, e.g. NORMAL_EXIT
     * @return {@code "RAN NORMALLY"}, {@code "EXCEPTION"} or {@code "OTHER"}
     */
    public String determineOutcome(String eventType){

        if(eventType.equals("NORMAL_EXIT")){

            return "RAN NORMALLY";
        }

        if(eventType.equals("EXCEPTION_EXIT")){

            return "EXCEPTION";
        }

        return "OTHER";
    }
}
