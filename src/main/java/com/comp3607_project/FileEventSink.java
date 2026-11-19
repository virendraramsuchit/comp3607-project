package com.comp3607_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.comp3607_project.report.JsonReportStrategy;
import com.comp3607_project.report.PdfReportStrategy;
import com.comp3607_project.report.ReportStrategy;

/**
 * Keeps every event of the session in memory. Each new event is appended to a plain-text log
 * and the JSON file is rewritten. The PDF report is written when {@code exportPdf()} is called
 * at the end of the session. Output is written to the user's home folder.
 */
public class FileEventSink implements EventSink {

        /**
     * Every event recorded this session, in order.
     */
    private final List<TelemetryEvent> events = new ArrayList<>();

        /**
     * Writes {@code telemetry-output.json}.
     */
    private final ReportStrategy jsonStrategy;

        /**
     * Writes {@code telemetry-report.pdf}.
     */
    private final ReportStrategy pdfStrategy;

        /**
     * {@code telemetry-output.txt} in the user's home folder.
     */
    private final Path txtFile;

        /**
     * Creates a {@code JsonReportStrategy} and a {@code PdfReportStrategy} and sets the text-log
     * path.
     */
    public FileEventSink() {
        this.jsonStrategy = new JsonReportStrategy();
        this.pdfStrategy  = new PdfReportStrategy();
        this.txtFile = Path.of(System.getProperty("user.home"), "telemetry-output.txt");
    }

        /**
     * *synchronized.* Adds the event, appends it to the text log and rewrites the JSON file.
     *
     * @param event the event to record
     */
    @Override
    public synchronized void record(TelemetryEvent event) {
        events.add(event);
        appendText(event);
        jsonStrategy.write(events);   // uses Strategy
    }

        /**
     * Appends the event's {@code toString()} as one line; write errors are ignored.
     *
     * @param e the event to record
     */
    private void appendText(TelemetryEvent e) {
        try {
            Files.writeString(txtFile, e.toString() + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) { /* ignore */  }
    }

        /**
     * Writes the PDF report from all events so far. Called by {@code TelemetryFacade.stop()}.
     */
   //Generates the final PDF. 
    public void exportPdf() {
        pdfStrategy.write(events);
    }
}
