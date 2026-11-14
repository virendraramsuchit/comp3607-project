package com.comp3607_project;

import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;

public class ExecutionListener implements InvocationFinishedListener {
    
    private EventSink sink;

    public ExecutionListener(EventSink sink){

        this.sink = sink;  
    }

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
