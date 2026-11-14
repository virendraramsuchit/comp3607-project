package com.comp3607_project;

//Where lifecycle events go once created. 
 
@FunctionalInterface
public interface LifecycleSink {
    void accept(LifecycleEvent event);
}