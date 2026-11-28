package com.unip.api.report;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.unip.api.report.dto.OverdueActionResponse;
import com.unip.api.report.dto.SummaryResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportPdfGeneratorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 10, 30);

    private final ReportPdfGenerator generator = new ReportPdfGenerator();

    private SummaryResponse summary() {
        return new SummaryResponse(3, Map.of("OPEN", 2L, "CLOSED", 1L),
                Map.of("LEAK", 3L), Map.of("Maintenance", 3L));
    }

    private OverdueActionResponse overdue(long id) {
        return new OverdueActionResponse(id, id, "Dripping tap", "Replace the gasket",
                "Employee Two", LocalDate.of(2026, 11, 10), 8);
    }

    private String text(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder all = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            all.append(extractor.getTextFromPage(page)).append('\n');
        }
        reader.close();
        return all.toString();
    }

    private int pages(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        int pages = reader.getNumberOfPages();
        reader.close();
        return pages;
    }

    @Test
    void producesAValidPdf() {
        byte[] pdf = generator.build(summary(), List.of(overdue(1)), NOW);

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void containsTitleTotalsAndOverdueData() throws Exception {
        String text = text(generator.build(summary(), List.of(overdue(1)), NOW));

        assertTrue(text.contains("Environmental Incident Report"));
        assertTrue(text.contains("Total incidents: 3"));
        assertTrue(text.contains("Maintenance"));
        assertTrue(text.contains("Replace the gasket"));
        assertTrue(text.contains("18/11/2026"));
    }

    @Test
    void emptyOverdueListShowsAMessage() throws Exception {
        String text = text(generator.build(summary(), List.of(), NOW));

        assertTrue(text.contains("No overdue actions."));
    }

    @Test
    void emptySummaryDoesNotBreak() throws Exception {
        SummaryResponse empty = new SummaryResponse(0, Map.of(), Map.of(), Map.of());

        String text = text(generator.build(empty, List.of(), NOW));

        assertTrue(text.contains("Total incidents: 0"));
        assertTrue(text.contains("No data."));
    }

    @Test
    void manyRowsSpillOverToMorePages() throws Exception {
        List<OverdueActionResponse> many = new ArrayList<>();
        for (long i = 1; i <= 120; i++) many.add(overdue(i));

        assertTrue(pages(generator.build(summary(), many, NOW)) > 1);
    }

    @Test
    void accentedTextDoesNotBreakGeneration() {
        SummaryResponse accented = new SummaryResponse(1, Map.of("OPEN", 1L),
                Map.of("LEAK", 1L), Map.of("Manutenção", 1L));

        byte[] pdf = generator.build(accented, List.of(), NOW);

        assertTrue(pdf.length > 0);
    }
}