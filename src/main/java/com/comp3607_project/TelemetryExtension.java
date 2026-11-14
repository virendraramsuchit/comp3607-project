package com.comp3607_project;

import java.io.IOException;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;

public class TelemetryExtension extends Extension {

    private TelemetryFacade facade;

    @Override
    public void startup(BlueJ bluej) {
        try {
            facade = new TelemetryFacade(bluej);
            facade.start();
        } catch (IOException e) {
            System.err.println("[Telemetry Tracker Extension] could not start: " + e.getMessage());
        }
    }

    @Override
    public void terminate() {
        if (facade != null) facade.stop();
    }

    @Override public String getName() { return "Telemetry Tracker Extension"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getDescription() { return "Tracks edits, execution, compilation, and lifecycle events."; }
    @Override public boolean isCompatible() { return true; }

}

