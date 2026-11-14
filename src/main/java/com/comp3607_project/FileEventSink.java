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

public class FileEventSink implements EventSink {

    private final List<TelemetryEvent> events = new ArrayList<>();
    private final ReportStrategy jsonStrategy;
    private final ReportStrategy pdfStrategy;
    private final Path txtFile;

    public FileEventSink() {
        this.jsonStrategy = new JsonReportStrategy();
        this.pdfStrategy  = new PdfReportStrategy();
        this.txtFile = Path.of(System.getProperty("user.home"), "telemetry-output.txt");
    }

    @Override
    public synchronized void record(TelemetryEvent event) {
        events.add(event);
        appendText(event);
        jsonStrategy.write(events);   // uses Strategy
    }

    private void appendText(TelemetryEvent e) {
        try {
            Files.writeString(txtFile, e.toString() + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) { /* ignore */  }
    }

   //Generates the final PDF. 
    public void exportPdf() {
        pdfStrategy.write(events);
    }
}
