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
/**
 * Once a second, checks the text of every open class editor in every open project. When a
 * class's text has changed since the last check, records an {@link EditEvent} with the number
 * of characters changed and the time since that class's previous edit.
 */
public class EditPoller {
/**
 * Where edit events are sent.
 */
    private EventSink sink;
/**
 * Used to find the open projects, packages and classes.
 */
    private BlueJ bluej;
/**
 * Works out how many characters changed.
 */
    private EditCalculator calculator;
/**
 * Last seen text of each class, keyed by {@code project/class}.
 */
    //Stores previous contents of each class
    private Map <String, String> previousContents;
/**
 * Time of the last recorded edit for each class, keyed by {@code project/class}.
 */
    private Map<String, Long> lastEditTime = new HashMap<>();
/**
 * Runs the once-a-second check.
 */
    //Runs the polling task
    private ScheduledExecutorService scheduler;
/**
 * Creates a poller that reads from {@code bluej} and sends events to {@code sink}.
 *
 * @param bluej the BlueJ proxy passed to the extension
 * @param sink where edit events are sent
 */
    public EditPoller(BlueJ bluej, EventSink sink){
        this.bluej = bluej;
        this.sink = sink;
        this.calculator = new EditCalculator();
        this.previousContents = new HashMap<>();
    }
/**
 * Compares each open editor's text with its previous text. The first time a class is seen, its
 * text is stored and nothing is recorded. On a change, records an {@code EditEvent} (idle time
 * is 0 for a class's first recorded edit). Errors are caught and printed so polling continues.
 */
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

/**
 * Starts a single background timer that runs {@code checkForChanges()} every second on the
 * JavaFX application thread ({@code Platform.runLater}), where editor text can be read.
 */
    public void start(){

        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(
            () -> Platform.runLater(this::checkForChanges), 
            0, 
            1, 
            TimeUnit.SECONDS
        );
    }
/**
 * Shuts down the timer.
 */
    public void stop(){

        if(scheduler != null){
            scheduler.shutdown();
        }
    }

}

