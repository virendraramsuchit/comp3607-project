package com.comp3607_project;

import java.io.IOException;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;

/**
 * The class BlueJ loads when the extension starts (named as {@code Main-Class} in the jar
 * manifest). It creates a {@link TelemetryFacade} on startup and stops it when BlueJ closes.
 * It contains no telemetry logic of its own.
 */
public class TelemetryExtension extends Extension {

        /**
     * The facade that runs the plugin. {@code null} until {@code startup} succeeds.
     */
    private TelemetryFacade facade;

        /**
     * Called by BlueJ when the extension loads. Creates the facade and calls {@code start()}. If
     * the facade cannot be created (an {@code IOException}), prints an error and the extension
     * stays inactive.
     *
     * @param bluej the BlueJ proxy passed to the extension
     */
    @Override
    public void startup(BlueJ bluej) {
        try {
            facade = new TelemetryFacade(bluej);
            facade.start();
        } catch (IOException e) {
            System.err.println("[Telemetry Tracker Extension] could not start: " + e.getMessage());
        }
    }

        /**
     * Called by BlueJ when it closes. Calls {@code facade.stop()} if the facade was created.
     */
    @Override
    public void terminate() {
        if (facade != null) facade.stop();
    }

        /**
     * Returns {@code "Telemetry Tracker Extension"}.
     * @return {@code "Telemetry Tracker Extension"}
     */
    @Override public String getName() { return "Telemetry Tracker Extension"; }

        /**
     * Returns {@code "1.0.0"}.
     * @return {@code "1.0.0"}
     */
    @Override public String getVersion() { return "1.0.0"; }

        /**
     * Returns a one-line description shown in BlueJ's Installed Extensions list.
     * @return a one-line description shown in BlueJ's Installed Extensions list
     */
    @Override public String getDescription() { return "Tracks edits, execution, compilation, and lifecycle events."; }

        /**
     * Always returns {@code true}.
     * @return always {@code true}
     */
    @Override public boolean isCompatible() { return true; }

}

