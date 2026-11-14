package com.comp3607_project.report;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.comp3607_project.ActivityEvent;
import com.comp3607_project.CompileEvent;
import com.comp3607_project.EditEvent;
import com.comp3607_project.ExecutionEvent;
import com.comp3607_project.LifecycleEvent;
import com.comp3607_project.LifecycleEventAdapter;
import com.comp3607_project.LifecycleEventType;
import com.comp3607_project.TelemetryEvent;

class PdfReportStrategyTest {

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

    private Path pdfPath() {
        return tempHome.resolve("telemetry-report.pdf");
    }

    @Test
    void writeCreatesPdfFile() {
        new PdfReportStrategy().write(List.of(new EditEvent("Foo", 3, 1000L)));
        assertTrue(Files.exists(pdfPath()));
    }

    @Test
    void writePdfIsNonEmpty() throws Exception {
        new PdfReportStrategy().write(List.of(new EditEvent("Foo", 3, 1000L)));
        assertTrue(Files.size(pdfPath()) > 0);
    }

    @Test
    void writeWithEmptyEventsDoesNotCrash() {
        assertDoesNotThrow(() -> new PdfReportStrategy().write(List.of()));
        assertTrue(Files.exists(pdfPath()));
    }

    @Test
    void writeWithAllEventTypesDoesNotCrash() {
        LifecycleEvent lifecycle = new LifecycleEvent(
            1000L, LifecycleEventType.CLASS_CREATED, "P", "/p", "", "Foo", null, null);

        List<TelemetryEvent> events = List.of(
            new EditEvent("Foo", 5, 1000L),
            new ExecutionEvent("Foo", "m", "EXCEPTION"),
            new CompileEvent(CompileEvent.Outcome.ERROR, "F.java", 1, "msg", "key"),
            new LifecycleEventAdapter(lifecycle),
            new ActivityEvent("pkg", 5000L, 3000L)
        );

        assertDoesNotThrow(() -> new PdfReportStrategy().write(events));
        assertTrue(Files.exists(pdfPath()));
    }
}
