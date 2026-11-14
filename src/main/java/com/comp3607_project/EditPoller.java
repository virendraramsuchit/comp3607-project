package com.comp3607_project;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.BlueJ;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.editor.TextLocation;
import javafx.application.Platform;

public class EditPoller {
    
    private EventSink sink;
    private BlueJ bluej;
    private EditCalculator calculator;

    //Stores previous contents of each class
    private Map <String, String> previousContents;

    private Map<String, Long> lastEditTime = new HashMap<>();

    //Runs the polling task
    private ScheduledExecutorService scheduler;

    public EditPoller(BlueJ bluej, EventSink sink){
        this.bluej = bluej;
        this.sink = sink;
        this.calculator = new EditCalculator();
        this.previousContents = new HashMap<>();
    }

    public void checkForChanges(){
    
        //Try - catch used since getPackages() and getClasses() throw a checked exception which needs to be caught
        try{ 
            for(BProject project : bluej.getOpenProjects()){
                
                for(BPackage pkg : project.getPackages()){

                    for(BClass bClass : pkg.getClasses()){

                        //Get Java editor only if open
                        JavaEditor editor = bClass.getJavaEditorIfOpen();

                        if(editor == null){
                            continue;
                        }

                        //Identify the class
                        String className = bClass.getName();
                        String key = project.getName() + "/" + className;

                        //Retrieve current editor contents
                        int length = editor.getTextLength();

                        TextLocation start = editor.getTextLocationFromOffset(0);
                        TextLocation end = editor.getTextLocationFromOffset(length);
                        String currentText = editor.getText(start, end);

                        //Retrieve previous contents
                        String oldText = previousContents.get(key);

                        //First time checking a new class
                        if(oldText == null){
                            previousContents.put(key, currentText);
                            continue;
                        }

                        //Check if contents changed
                        if(calculator.isChanged(oldText, currentText)){

                            int changes = calculator.getNumChanges(oldText, currentText);

                            long now  = System.currentTimeMillis();
                            long idle = now - lastEditTime.getOrDefault(key, now);

                            sink.record(new EditEvent(className, changes, idle));
                            lastEditTime.put(key, now);
                        }

                        //Update previous contents
                        previousContents.put(key, currentText);
                    }
                }
            }
        }catch (Exception e){
            System.err.println("Error polling for edits: " + e.getMessage());
        }

    }


    public void start(){

        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(
            () -> Platform.runLater(this::checkForChanges), 
            0, 
            1, 
            TimeUnit.SECONDS
        );
    }

    public void stop(){

        if(scheduler != null){
            scheduler.shutdown();
        }
    }

}

