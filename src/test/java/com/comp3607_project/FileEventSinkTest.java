package com.comp3607_project;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileEventSinkTest {

    @TempDir
    Path tempHome;

    private String originalHome;

    @BeforeEach
    void redirectHome() {
        originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());
    }

    @AfterEach
    void restoreHome() {
        System.setProperty("user.home", originalHome);
    }

    @Test
    void recordWritesTextFile() throws Exception {
        FileEventSink sink = new FileEventSink();
        sink.record(new EditEvent("Foo", 3, 1000L));

        Path txt = tempHome.resolve("telemetry-output.txt");
        assertTrue(Files.exists(txt));
        assertTrue(Files.readString(txt).contains("class=Foo"));
    }

    @Test
    void recordWritesJsonFile() throws Exception {
        FileEventSink sink = new FileEventSink();
        sink.record(new EditEvent("Foo", 3, 1000L));

        Path json = tempHome.resolve("telemetry-output.json");
        assertTrue(Files.exists(json));
        String content = Files.readString(json);
        assertTrue(content.contains("\"type\":\"EDIT\""));
        assertTrue(content.contains("\"class\":\"Foo\""));
    }

    @Test
    void recordMultipleEventsAllAppearInJson() throws Exception {
        FileEventSink sink = new FileEventSink();
        sink.record(new EditEvent("A", 1, 100L));
        sink.record(new ExecutionEvent("B", "m", "RAN NORMALLY"));

        String json = Files.readString(tempHome.resolve("telemetry-output.json"));
        assertTrue(json.contains("\"class\":\"A\""));
        assertTrue(json.contains("\"class\":\"B\""));
    }

    @Test
    void exportPdfCreatesFile() {
        FileEventSink sink = new FileEventSink();
        sink.record(new EditEvent("Foo", 3, 1000L));
        sink.exportPdf();

        Path pdf = tempHome.resolve("telemetry-report.pdf");
        assertTrue(Files.exists(pdf));
    }

    @Test
    void exportPdfWithNoEventsStillCreatesFile() {
        FileEventSink sink = new FileEventSink();
        sink.exportPdf();
        assertTrue(Files.exists(tempHome.resolve("telemetry-report.pdf")));
    }
}