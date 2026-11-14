package com.example;

//Where lifecycle events go once created. 
 
@FunctionalInterface
public interface LifecycleSink {
    void accept(LifecycleEvent event);
}