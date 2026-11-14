package com.comp3607_project.report;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
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

class JsonReportStrategyTest {

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

    private String readJson() throws Exception {
        return Files.readString(tempHome.resolve("telemetry-output.json"));
    }

    @Test
    void writeCreatesJsonFile() {
        new JsonReportStrategy().write(List.of(new EditEvent("Foo", 3, 1000L)));
        assertTrue(Files.exists(tempHome.resolve("telemetry-output.json")));
    }

    @Test
    void writeEmptyListProducesEmptySummary() throws Exception {
        new JsonReportStrategy().write(List.of());
        assertTrue(readJson().contains("\"summary\": {}"));
    }

    @Test
    void editEventSerialized() throws Exception {
        new JsonReportStrategy().write(List.of(new EditEvent("Foo", 7, 1500L)));
        String json = readJson();
        assertTrue(json.contains("\"type\":\"EDIT\""));
        assertTrue(json.contains("\"charsChanged\":7"));
        assertTrue(json.contains("\"idleMs\":1500"));
    }

    @Test
    void executionEventSerializedWithConstructorFallback() throws Exception {
        new JsonReportStrategy().write(List.of(new ExecutionEvent("Foo", null, "RAN NORMALLY")));
        assertTrue(readJson().contains("\"method\":\"constructor\""));
    }

    @Test
    void lifecycleEventSerializedViaAdapter() throws Exception {
        LifecycleEvent original = new LifecycleEvent(
            1000L, LifecycleEventType.CLASS_CREATED, "P", "/p", "pkg", "Foo", null, "test");
        new JsonReportStrategy().write(List.of(new LifecycleEventAdapter(original)));
        String json = readJson();
        assertTrue(json.contains("\"type\":\"LIFECYCLE\""));
        assertTrue(json.contains("\"eventType\":\"CLASS_CREATED\""));
    }

    @Test
    void compileEventSerialized() throws Exception {
        new JsonReportStrategy().write(List.of(
            new CompileEvent(CompileEvent.Outcome.ERROR, "Foo.java", 12,
                            "cannot find symbol", "cannot find symbol")));
        String json = readJson();
        assertTrue(json.contains("\"type\":\"COMPILE\""));
        assertTrue(json.contains("\"outcome\":\"ERROR\""));
        assertTrue(json.contains("\"file\":\"Foo.java\""));
    }

    @Test
    void activityEventSerialized() throws Exception {
        new JsonReportStrategy().write(List.of(new ActivityEvent("pkg", 5000L, 3000L)));
        String json = readJson();
        assertTrue(json.contains("\"type\":\"ACTIVITY\""));
        assertTrue(json.contains("\"activeMs\":5000"));
        assertTrue(json.contains("\"idleMs\":3000"));
    }

    @Test
    void summaryCountsEditsAndIdle() throws Exception {
        new JsonReportStrategy().write(List.of(
            new EditEvent("Foo", 5, 1000L),
            new EditEvent("Foo", 3, 2000L)
        ));
        String json = readJson();
        assertTrue(json.contains("\"totalEdits\": 2"));
        assertTrue(json.contains("\"idleMs\": 3000"));
    }

    @Test
    void summaryCountsRunsAndExceptions() throws Exception {
        new JsonReportStrategy().write(List.of(
            new ExecutionEvent("Foo", "a", "RAN NORMALLY"),
            new ExecutionEvent("Foo", "b", "EXCEPTION")
        ));
        String json = readJson();
        assertTrue(json.contains("\"totalRuns\": 2"));
        assertTrue(json.contains("\"totalExceptions\": 1"));
        assertTrue(json.contains("\"exceptionRatePercent\": 50.00"));
    }

    @Test
    void summaryCountsCompilation() throws Exception {
        new JsonReportStrategy().write(List.of(
            new CompileEvent(CompileEvent.Outcome.SUCCESS, null, 0, null, null),
            new CompileEvent(CompileEvent.Outcome.FAILURE, null, 0, null, null),
            new CompileEvent(CompileEvent.Outcome.ERROR, "F.java", 1, "x", "cannot find symbol")
        ));
        String json = readJson();
        assertTrue(json.contains("\"successes\": 1"));
        assertTrue(json.contains("\"failures\": 1"));
        assertTrue(json.contains("\"cannot find symbol\":1"));
    }

    @Test
    void summaryOnlyEditsNoRunsYieldsZeroExceptionRate() throws Exception {
        new JsonReportStrategy().write(List.of(new EditEvent("A", 5, 100L)));
        String json = readJson();
        assertTrue(json.contains("\"totalRuns\": 0"));
        assertTrue(json.contains("\"exceptionRatePercent\": 0.00"));
    }

    @Test
    void unknownEventTypeProducesUnknownJson() throws Exception {
        TelemetryEvent unknown = () -> 100L;
        new JsonReportStrategy().write(List.of(unknown));
        assertTrue(readJson().contains("\"type\":\"UNKNOWN\""));
    }
}