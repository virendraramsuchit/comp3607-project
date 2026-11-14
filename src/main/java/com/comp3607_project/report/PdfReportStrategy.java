package com.comp3607_project.report;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.comp3607_project.ActivityEvent;
import com.comp3607_project.CompileEvent;
import com.comp3607_project.EditEvent;
import com.comp3607_project.ExecutionEvent;
import com.comp3607_project.LifecycleEventAdapter;
import com.comp3607_project.TelemetryEvent;

public class PdfReportStrategy implements ReportStrategy {

    private final Path pdfFile;

    public PdfReportStrategy() {
        this.pdfFile = Path.of(System.getProperty("user.home"), "telemetry-report.pdf");
    }

    
    @Override
    public void write(List<TelemetryEvent> events) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDFont bold  = PDType1Font.HELVETICA_BOLD;
                PDFont plain = PDType1Font.HELVETICA;

                // ---- Aggregate all data once ----
                long dur = events.isEmpty() ? 0
                        : events.get(events.size() - 1).getTimeStamp() - events.get(0).getTimeStamp();

                long pollIdleMs = 0;
                int edits = 0, runs = 0, exc = 0;
                int compileSuccess = 0, compileFailure = 0;
                int lifecycleTotal = 0;
                long lastActive = 0, lastIdle = 0;

                Map<String,Integer> eC = new HashMap<>();
                Map<String,Integer> rC = new HashMap<>();
                Map<String,Integer> xC = new HashMap<>();
                Map<String,Integer> lT = new HashMap<>();
                Map<String,Integer> compileErrorCounts = new HashMap<>();

                for (TelemetryEvent e : events) {
                    if (e instanceof EditEvent ed) {
                        edits++;
                        pollIdleMs += ed.getIdleMillis();
                        eC.merge(ed.getClassName(), ed.getNumCharsChanged(), Integer::sum);
                    } else if (e instanceof ExecutionEvent ex) {
                        runs++;
                        rC.merge(ex.getClassName(), 1, Integer::sum);
                        if ("EXCEPTION".equals(ex.getOutcome())) {
                            exc++;
                            xC.merge(ex.getClassName(), 1, Integer::sum);
                        }
                    } else if (e instanceof LifecycleEventAdapter la) {
                        lifecycleTotal++;
                        lT.merge(la.getWrapped().getType().name(), 1, Integer::sum);
                    } else if (e instanceof CompileEvent ce) {
                        switch (ce.getOutcome()) {
                            case SUCCESS -> compileSuccess++;
                            case FAILURE -> compileFailure++;
                            case ERROR   -> compileErrorCounts.merge(ce.getErrorKey(), 1, Integer::sum);
                            default -> { }
                        }
                    } else if (e instanceof ActivityEvent ae) {
                        lastActive = ae.getActiveMillis();
                        lastIdle   = ae.getIdleMillis();
                    }
                }
                long pollActiveMs = Math.max(0, dur - pollIdleMs);

                // ---- 1. Header + session stats ----
                float y = 800;
                cs.beginText(); cs.setFont(bold, 20);
                cs.newLineAtOffset(50, y); cs.showText("BlueJ Telemetry Report");
                cs.endText(); y -= 40;

                String[] stats = {
                    String.format("Session duration : %.1f s", dur / 1000.0),
                    String.format("Active (session) : %.1f s", pollActiveMs / 1000.0),
                    String.format("Idle   (session) : %.1f s (%.1f%%)",
                        pollIdleMs / 1000.0,
                        dur == 0 ? 0 : 100.0 * pollIdleMs / dur),
                    String.format("Typing active    : %.1f s (threshold-based)", lastActive / 1000.0),
                    String.format("Typing idle      : %.1f s", lastIdle / 1000.0),
                    String.format("Edit events      : %d", edits),
                };
                for (String s : stats) {
                    cs.beginText(); cs.setFont(plain, 12);
                    cs.newLineAtOffset(50, y); cs.showText(s);
                    cs.endText(); y -= 18;
                }
                y -= 20;

                // ---- 2. Lifecycle events ----
                cs.beginText(); cs.setFont(bold, 16);
                cs.newLineAtOffset(50, y); cs.showText("Lifecycle events"); cs.endText();
                y -= 22;
                cs.beginText(); cs.setFont(plain, 10);
                cs.newLineAtOffset(50, y);
                cs.showText(String.format("Total: %d", lifecycleTotal));
                cs.endText(); y -= 16;
                y = table(cs, bold, plain, "By type", lT, 50, y);

                // ---- 3. Compilation summary ----
                y -= 20;
                cs.beginText(); cs.setFont(bold, 16);
                cs.newLineAtOffset(50, y); cs.showText("Compilation summary"); cs.endText();
                y -= 22;
                String[] comp = {
                    String.format("Successful compiles : %d", compileSuccess),
                    String.format("Failed compiles     : %d", compileFailure),
                };
                for (String s : comp) {
                    cs.beginText(); cs.setFont(plain, 10);
                    cs.newLineAtOffset(60, y); cs.showText(s); cs.endText(); y -= 14;
                }
                y = table(cs, bold, plain, "Error breakdown", compileErrorCounts, 50, y - 4);

                // ---- 4. Runtime stats ----
                y -= 20;
                cs.beginText(); cs.setFont(bold, 16);
                cs.newLineAtOffset(50, y); cs.showText("Runtime stats"); cs.endText();
                y -= 22;
                String[] runtime = {
                    String.format("Program runs     : %d", runs),
                    String.format("Exceptions       : %d (%.1f%%)",
                        exc, runs == 0 ? 0 : 100.0 * exc / runs),
                };
                for (String s : runtime) {
                    cs.beginText(); cs.setFont(plain, 10);
                    cs.newLineAtOffset(60, y); cs.showText(s); cs.endText(); y -= 14;
                }
                y = table(cs, bold, plain, "Edits per class",      eC, 50, y - 4);
                y = table(cs, bold, plain, "Runs per class",       rC, 50, y - 4);
                y = table(cs, bold, plain, "Exceptions per class", xC, 50, y - 4);

                // ---- 5. Charts ----
                if (y < 300) {
                    cs.close();
                    PDPage page2 = new PDPage(PDRectangle.A4);
                    doc.addPage(page2);
                    try (PDPageContentStream cs2 = new PDPageContentStream(doc, page2)) {
                        drawGraphSection(cs2, bold, plain, 780,
                                        rC, xC, compileErrorCounts, lT);
                    }
                } else {
                    drawGraphSection(cs, bold, plain, y - 40,
                                    rC, xC, compileErrorCounts, lT);
                }
            }
            doc.save(pdfFile.toFile());
        } catch (IOException e) {
            System.err.println("PDF write failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private float table(PDPageContentStream cs, PDFont bold, PDFont plain,
                        String title, Map<String,Integer> data, float x, float y) throws IOException {
        cs.beginText(); cs.setFont(bold, 13);
        cs.newLineAtOffset(x, y); cs.showText(title); cs.endText(); y -= 18;
        if (data.isEmpty()) {
            cs.beginText(); cs.setFont(plain, 10);
            cs.newLineAtOffset(x + 10, y); cs.showText("(none)"); cs.endText();
            return y - 15;
        }
        for (var e : data.entrySet()) {
            cs.beginText(); cs.setFont(plain, 10);
            cs.newLineAtOffset(x + 10, y);
            cs.showText(String.format("%-25s %d", e.getKey(), e.getValue()));
            cs.endText(); y -= 13;
        }
        return y;
    }

    private void drawGraphSection(PDPageContentStream cs, PDFont bold, PDFont plain,
                                  float startY,
                                  Map<String,Integer> rC,
                                  Map<String,Integer> xC,
                                  Map<String,Integer> compileErrors,
                                  Map<String,Integer> lifecycleTypes) throws IOException {
        float y = startY;

        cs.beginText(); cs.setFont(bold, 16);
        cs.newLineAtOffset(50, y); cs.showText("Charts"); cs.endText();
        y -= 30;

        // Chart 1: runs vs exceptions per class
        Map<String,int[]> stacks = new java.util.LinkedHashMap<>();
        java.util.Set<String> allClasses = new java.util.TreeSet<>();
        allClasses.addAll(rC.keySet());
        allClasses.addAll(xC.keySet());
        for (String c : allClasses) {
            stacks.put(c, new int[]{ rC.getOrDefault(c, 0), xC.getOrDefault(c, 0) });
        }
        drawStackedHBar(cs, bold, plain, "Run outcomes per class",
                        stacks, 50, y, 420, 20);
        y -= (40 + Math.max(1, stacks.size()) * 32);

        // Chart 2: compilation errors by type
        drawSegmentedBar(cs, bold, plain, "Compilation errors by type",
                         compileErrors, 50, y, 420, 20);
        y -= (60 + Math.max(1, (compileErrors.size() + 1) / 2) * 14);

        // Chart 3: lifecycle events by type
        drawSegmentedBar(cs, bold, plain, "Lifecycle events by type",
                         lifecycleTypes, 50, y, 420, 20);
    }

    private void drawStackedHBar(PDPageContentStream cs, PDFont bold, PDFont plain,
                                 String title, Map<String,int[]> rows,
                                 float x, float y, float width, float barHeight) throws IOException {
        cs.beginText(); cs.setFont(bold, 12);
        cs.newLineAtOffset(x, y); cs.showText(title); cs.endText();
        y -= 18;

        cs.setNonStrokingColor(60/255f, 130/255f, 200/255f);
        cs.addRect(x, y - 2, 10, 8); cs.fill();
        cs.beginText(); cs.setFont(plain, 9);
        cs.newLineAtOffset(x + 14, y); cs.showText("Runs %"); cs.endText();

        cs.setNonStrokingColor(200/255f, 70/255f, 60/255f);
        cs.addRect(x + 65, y - 2, 10, 8); cs.fill();
        cs.beginText(); cs.setFont(plain, 9);
        cs.newLineAtOffset(x + 79, y); cs.showText("Exceptions %"); cs.endText();

        y -= 22;

        if (rows.isEmpty()) {
            cs.beginText(); cs.setFont(plain, 10);
            cs.newLineAtOffset(x, y); cs.showText("(no data)"); cs.endText();
            return;
        }

        float labelWidth = 80;
        float barX       = x + labelWidth;
        float barW       = width - labelWidth;
        float gap        = 10;
        float currentY   = y;
        float minY       = 40;

        int[][] colors = {
            {60, 130, 200},
            {200, 70, 60}
        };

        for (var entry : rows.entrySet()) {
            if (currentY < minY) {
                cs.beginText(); cs.setFont(plain, 9);
                cs.newLineAtOffset(x, currentY);
                cs.showText("(more rows omitted — page full)");
                cs.endText();
                return;
            }

            String cls = entry.getKey();
            int[] counts = entry.getValue();
            int total = counts[0] + counts[1];
            if (total == 0) continue;

            cs.beginText(); cs.setFont(plain, 10);
            cs.newLineAtOffset(x, currentY + barHeight - 6);
            cs.showText(cls);
            cs.endText();

            float cursor = barX;
            for (int i = 0; i < 2; i++) {
                if (counts[i] == 0) continue;
                float segW = barW * counts[i] / (float) total;
                cs.setNonStrokingColor(
                    colors[i][0]/255f, colors[i][1]/255f, colors[i][2]/255f);
                cs.addRect(cursor, currentY, segW, barHeight);
                cs.fill();
                cursor += segW;
            }

            cs.setStrokingColor(180, 180, 180);
            cs.setLineWidth(0.5f);
            cs.addRect(barX, currentY, barW, barHeight);
            cs.stroke();

            cs.setNonStrokingColor(0, 0, 0);
            cs.beginText(); cs.setFont(plain, 9);
            cs.newLineAtOffset(barX + barW + 6, currentY + barHeight - 6);
            int pct = Math.round(100f * counts[0] / total);
            cs.showText(pct + "% / " + (100 - pct) + "%");
            cs.endText();

            currentY -= (barHeight + gap);
        }
    }

    private void drawSegmentedBar(PDPageContentStream cs, PDFont bold, PDFont plain,
                                  String title, Map<String,Integer> data,
                                  float x, float y, float width, float barHeight) throws IOException {
        cs.beginText(); cs.setFont(bold, 12);
        cs.newLineAtOffset(x, y); cs.showText(title); cs.endText();
        y -= 22;

        if (data.isEmpty()) {
            cs.beginText(); cs.setFont(plain, 10);
            cs.newLineAtOffset(x, y); cs.showText("(no data)"); cs.endText();
            return;
        }

        int total = data.values().stream().mapToInt(Integer::intValue).sum();

        int[][] palette = {
            {80, 140, 90},
            {200, 150, 60},
            {140, 100, 180},
            {60, 140, 200},
            {200, 80, 80},
            {120, 120, 120},
        };

        float cursor = x;
        int i = 0;
        for (var entry : data.entrySet()) {
            float segW = width * entry.getValue() / (float) total;
            int[] c = palette[i % palette.length];
            cs.setNonStrokingColor(c[0]/255f, c[1]/255f, c[2]/255f);
            cs.addRect(cursor, y, segW, barHeight);
            cs.fill();
            cursor += segW;
            i++;
        }

        cs.setStrokingColor(180, 180, 180);
        cs.setLineWidth(0.5f);
        cs.addRect(x, y, width, barHeight);
        cs.stroke();

        y -= 22;
        float legendX = x;
        int col = 0;
        i = 0;
        for (var entry : data.entrySet()) {
            int[] c = palette[i % palette.length];
            cs.setNonStrokingColor(c[0]/255f, c[1]/255f, c[2]/255f);
            cs.addRect(legendX, y, 8, 8);
            cs.fill();

            cs.setNonStrokingColor(0, 0, 0);
            cs.beginText(); cs.setFont(plain, 8);
            cs.newLineAtOffset(legendX + 12, y + 1);
            String name = entry.getKey();
            if (name.length() > 22) name = name.substring(0, 21) + "…";
            cs.showText(String.format("%s (%d)", name, entry.getValue()));
            cs.endText();

            col++;
            if (col == 2) {
                legendX = x;
                y -= 14;
                col = 0;
            } else {
                legendX = x + 210;
            }
            i++;
        }
    }


}
